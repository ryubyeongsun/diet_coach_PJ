<template>
  <div ref="canvasContainer" class="canvas-container"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from "vue";
import * as THREE from "three";
import { GLTFLoader } from "three/examples/jsm/loaders/GLTFLoader.js";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";

const props = defineProps({
  level: {
    type: Number,
    required: true,
    default: 3,
  },
});

const canvasContainer = ref(null);

// Three.js variables
let scene = null;
let camera = null;
let renderer = null;
let controls = null; // Add controls variable
let currentModel = null;
let animationId = null;
let loader = null;

// Mapping for levels (Color/Size fallback if GLB missing)
const LEVEL_CONFIG = {
  1: { color: 0x87CEEB, scaleX: 0.6, label: "매우 마름" },
  2: { color: 0xFFD700, scaleX: 0.8, label: "마름" },
  3: { color: 0x90EE90, scaleX: 1.0, label: "정상" },
  4: { color: 0xFFA07A, scaleX: 1.3, label: "통통" },
  5: { color: 0xFF6347, scaleX: 1.6, label: "비만" },
};

const initThree = () => {
  if (!canvasContainer.value) return;

  // 1. Scene
  scene = new THREE.Scene();
  scene.background = null; // Transparent background

  // 2. Camera
  const width = canvasContainer.value.clientWidth;
  const height = canvasContainer.value.clientHeight;
  camera = new THREE.PerspectiveCamera(50, width / height, 0.1, 100);
  camera.position.set(0, 1.0, 5.0); // 카메라를 조금 더 뒤로 빼고 높이 조절

  // 3. Renderer
  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true }); // alpha: true ensures transparency
  renderer.setSize(width, height);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.shadowMap.enabled = true;
  renderer.outputColorSpace = THREE.SRGBColorSpace; // PRD 4.1
  renderer.toneMapping = THREE.NoToneMapping;     // PRD 4.1
  renderer.toneMappingExposure = 1.0;             // PRD 4.1
  canvasContainer.value.appendChild(renderer.domElement);
  
  // --- Add Floor Shadow ---
  const planeGeometry = new THREE.PlaneGeometry(10, 10);
  const planeMaterial = new THREE.ShadowMaterial({ opacity: 0.1 }); // PRD 4.4: Adjusted opacity
  const plane = new THREE.Mesh(planeGeometry, planeMaterial);
  plane.rotation.x = -Math.PI / 2;
  plane.position.y = -1.5; // Match character's lowest Y position
  plane.receiveShadow = true;
  scene.add(plane);

  // 4. OrbitControls (Add horizontal rotation only)
  controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  controls.dampingFactor = 0.05;
  controls.enablePan = false;
  controls.enableZoom = false;
  controls.autoRotate = true;
  controls.autoRotateSpeed = 2.0;
  
  // 카메라가 캐릭터의 중앙(배/가슴 높이)을 바라보게 설정
  controls.target.set(0, 0.5, 0);
  
  // 수평 회전만 가능하도록 상하 각도 고정 (정면 90도)
  controls.minPolarAngle = Math.PI / 2; 
  controls.maxPolarAngle = Math.PI / 2;

  // 5. Lights
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.9); // Increased intensity slightly
  scene.add(ambientLight);

  const hemiLight = new THREE.HemisphereLight(0xffffff, 0x444444, 0.7); // PRD 4.3: Add HemisphereLight
  scene.add(hemiLight);

  const dirLight = new THREE.DirectionalLight(0xffffff, 1);
  dirLight.position.set(5, 10, 7);
  dirLight.castShadow = true;
  dirLight.shadow.mapSize.width = 1024;
  dirLight.shadow.mapSize.height = 1024;
  dirLight.shadow.bias = -0.0001;      // PRD 4.4
  dirLight.shadow.normalBias = 0.02;   // PRD 4.4
  scene.add(dirLight);

  // 6. Loader
  loader = new GLTFLoader();

  // 7. Initial Load
  loadCharacter(props.level);

  // 8. Animation Loop
  animate();
};

const loadCharacter = (level) => {
  const modelPath = `/models/avo/avoLV${level}.glb`;

  if (currentModel) {
    scene.remove(currentModel);
    currentModel.traverse((child) => {
      if (child.isMesh) {
        child.geometry.dispose();
        if (child.material.isMaterial) child.material.dispose();
      }
    });
    currentModel = null;
  }

  loader.load(
    modelPath,
    (gltf) => {
      currentModel = gltf.scene;
      currentModel.scale.set(1, 1, 1);
      
      // PRD 4.2: Texture Color Space Correction
      currentModel.traverse((child) => {
        if (child.isMesh) {
          child.castShadow = true;
          child.receiveShadow = true;
          
          if (child.material) {
            // Ensure textures are treated as sRGB
            if (child.material.map) child.material.map.colorSpace = THREE.SRGBColorSpace;
            if (child.material.emissiveMap) child.material.emissiveMap.colorSpace = THREE.SRGBColorSpace;
            child.material.needsUpdate = true;
          }
        }
      });

      const box = new THREE.Box3().setFromObject(currentModel);
      const center = box.getCenter(new THREE.Vector3());
      currentModel.position.x += currentModel.position.x - center.x;
      currentModel.position.y = -1.5; // 캐릭터를 더 아래로 내림
      currentModel.position.z += currentModel.position.z - center.z;

      scene.add(currentModel);
    },
    undefined,
    (error) => {
      console.warn(`Failed to load model at ${modelPath}. Using fallback.`, error);
      loadFallbackCharacter(level);
    }
  );
};

const loadFallbackCharacter = (level) => {
  const config = LEVEL_CONFIG[level] || LEVEL_CONFIG[3];
  const group = new THREE.Group();

  const geometry = new THREE.SphereGeometry(1, 32, 32);
  const material = new THREE.MeshStandardMaterial({ 
    color: config.color,
    roughness: 0.5,
    metalness: 0.1 
  });
  
  const body = new THREE.Mesh(geometry, material);
  body.scale.set(config.scaleX, 1, 0.8);
  body.castShadow = true;
  group.add(body);
  
  const eyeGeo = new THREE.SphereGeometry(0.1, 16, 16);
  const eyeMat = new THREE.MeshBasicMaterial({ color: 0x000000 });
  const leftEye = new THREE.Mesh(eyeGeo, eyeMat);
  leftEye.position.set(-0.3 * config.scaleX, 0.2, 0.7);
  group.add(leftEye);
  const rightEye = new THREE.Mesh(eyeGeo, eyeMat);
  rightEye.position.set(0.3 * config.scaleX, 0.2, 0.7);
  group.add(rightEye);

  group.position.y = 0;
  currentModel = group;
  currentModel.position.y = -0.2; // 폴백 모델 위치도 조정
  scene.add(currentModel);
};

const animate = () => {
  animationId = requestAnimationFrame(animate);
  
  if (controls) {
    controls.update(); // Let OrbitControls handle rotation
  }
  
  renderer.render(scene, camera);
};

const handleResize = () => {
  if (!canvasContainer.value || !camera || !renderer) return;
  const width = canvasContainer.value.clientWidth;
  const height = canvasContainer.value.clientHeight;
  
  camera.aspect = width / height;
  camera.updateProjectionMatrix();
  renderer.setSize(width, height);
};

watch(() => props.level, (newLevel) => {
  loadCharacter(newLevel);
});

onMounted(() => {
  initThree();
  window.addEventListener("resize", handleResize);
});

onUnmounted(() => {
  window.removeEventListener("resize", handleResize);
  cancelAnimationFrame(animationId);
  if (renderer) renderer.dispose();
});
</script>

<style scoped>
.canvas-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>