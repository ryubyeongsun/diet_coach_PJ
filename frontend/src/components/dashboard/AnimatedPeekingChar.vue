<template>
  <div ref="container" class="canvas-container"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import * as THREE from 'three';

const container = ref(null);

let scene, camera, renderer, animationId;
let characterGroup, leftArm, rightArm;
let clock = new THREE.Clock();

const init = () => {
  if (!container.value) return;

  // 1. Scene
  scene = new THREE.Scene();
  // Transparent background
  scene.background = null; 

  // 2. Camera
  const width = container.value.clientWidth;
  const height = container.value.clientHeight;
  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 100);
  camera.position.set(0, 0, 5);

  // 3. Renderer
  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setSize(width, height);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  container.value.appendChild(renderer.domElement);

  // 4. Light
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.7);
  scene.add(ambientLight);
  const dirLight = new THREE.DirectionalLight(0xffffff, 0.8);
  dirLight.position.set(2, 5, 5);
  scene.add(dirLight);

  // 5. Build Character (Procedural)
  createCharacter();

  // 6. Start Animation
  animate();
};

const createCharacter = () => {
  characterGroup = new THREE.Group();

  // -- Colors --
  const bodyColor = 0x10b981; // Emerald Green
  const bellyColor = 0xd1fae5; // Light Green
  
  // A. Body (Sphere)
  const bodyGeo = new THREE.SphereGeometry(1, 32, 32);
  const bodyMat = new THREE.MeshStandardMaterial({ color: bodyColor, roughness: 0.3 });
  const body = new THREE.Mesh(bodyGeo, bodyMat);
  characterGroup.add(body);

  // B. Belly (Circle Plane slightly in front)
  const bellyGeo = new THREE.CircleGeometry(0.7, 32);
  const bellyMat = new THREE.MeshBasicMaterial({ color: bellyColor });
  const belly = new THREE.Mesh(bellyGeo, bellyMat);
  belly.position.z = 0.95;
  belly.position.y = -0.1;
  characterGroup.add(belly);

  // C. Eyes (Black Spheres)
  const eyeGeo = new THREE.SphereGeometry(0.12, 16, 16);
  const eyeMat = new THREE.MeshBasicMaterial({ color: 0x111827 });
  
  const leftEye = new THREE.Mesh(eyeGeo, eyeMat);
  leftEye.position.set(-0.35, 0.2, 0.92);
  characterGroup.add(leftEye);

  const rightEye = new THREE.Mesh(eyeGeo, eyeMat);
  rightEye.position.set(0.35, 0.2, 0.92);
  characterGroup.add(rightEye);

  // D. Blush (Pink Circles)
  const blushGeo = new THREE.CircleGeometry(0.1, 16);
  const blushMat = new THREE.MeshBasicMaterial({ color: 0xf472b6, opacity: 0.6, transparent: true });
  
  const leftBlush = new THREE.Mesh(blushGeo, blushMat);
  leftBlush.position.set(-0.5, 0.05, 0.88);
  leftBlush.rotation.y = -0.4;
  characterGroup.add(leftBlush);

  const rightBlush = new THREE.Mesh(blushGeo, blushMat);
  rightBlush.position.set(0.5, 0.05, 0.88);
  rightBlush.rotation.y = 0.4;
  characterGroup.add(rightBlush);

  // E. Arms (Capsules/Cylinders) - Pivoted at shoulder
  const armGeo = new THREE.CapsuleGeometry(0.15, 0.5, 4, 8);
  const armMat = new THREE.MeshStandardMaterial({ color: bodyColor });

  // Left Arm Group (For pivoting)
  const leftArmGroup = new THREE.Group();
  leftArmGroup.position.set(-0.9, 0.1, 0); // Shoulder position
  
  leftArm = new THREE.Mesh(armGeo, armMat);
  leftArm.position.y = -0.25; // Offset so pivot is at top
  leftArm.rotation.z = Math.PI / 4; // Initial pose
  leftArmGroup.add(leftArm);
  characterGroup.add(leftArmGroup);
  
  // Right Arm Group
  const rightArmGroup = new THREE.Group();
  rightArmGroup.position.set(0.9, 0.1, 0); // Shoulder position

  rightArm = new THREE.Mesh(armGeo, armMat);
  rightArm.position.y = -0.25;
  rightArm.rotation.z = -Math.PI / 4;
  rightArmGroup.add(rightArm);
  characterGroup.add(rightArmGroup);

  // Save references to groups for animation
  characterGroup.userData = { leftArmGroup, rightArmGroup };

  // Initial Scale
  characterGroup.scale.set(1.3, 1.3, 1.3);
  
  // Look slightly up
  characterGroup.rotation.x = -0.2;

  scene.add(characterGroup);
};

const animate = () => {
  animationId = requestAnimationFrame(animate);
  const time = clock.getElapsedTime();

  if (characterGroup) {
    // 1. Body Floating (Bobbing)
    characterGroup.position.y = Math.sin(time * 2) * 0.1;

    // 2. Arms Waving
    // Left arm moves faster
    const { leftArmGroup, rightArmGroup } = characterGroup.userData;
    
    // Wave motion: Rotate around Z axis at shoulder
    // Math.sin(time * speed) * amplitude + baseOffset
    leftArmGroup.rotation.z = Math.sin(time * 8) * 0.5; 
    rightArmGroup.rotation.z = Math.cos(time * 8) * 0.5;
  }

  renderer.render(scene, camera);
};

const handleResize = () => {
  if (!container.value || !camera || !renderer) return;
  const width = container.value.clientWidth;
  const height = container.value.clientHeight;
  camera.aspect = width / height;
  camera.updateProjectionMatrix();
  renderer.setSize(width, height);
};

onMounted(() => {
  init();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
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