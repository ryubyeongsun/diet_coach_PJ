package com.dietcoach.project.controller;

import java.time.LocalDate;

import java.util.List;



import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.web.bind.annotation.*;



import com.dietcoach.project.common.ApiResponse;

import com.dietcoach.project.dto.weight.BodyStatusResponse;

import com.dietcoach.project.dto.weight.WeightRecordResponse;

import com.dietcoach.project.dto.weight.WeightRecordUpsertRequest;

import com.dietcoach.project.service.WeightRecordService;



import lombok.RequiredArgsConstructor;



@RestController

@RequiredArgsConstructor

@RequestMapping("/api/users/{userId}/weights")

public class WeightRecordController {



    private final WeightRecordService weightRecordService;



    @PostMapping

    public ApiResponse<WeightRecordResponse> upsertWeight(

            @PathVariable Long userId,

            @RequestBody WeightRecordUpsertRequest request

    ) {

        return ApiResponse.success(weightRecordService.upsertWeightRecord(userId, request));

    }



    @GetMapping

    public ApiResponse<List<WeightRecordResponse>> getWeights(

            @PathVariable Long userId,

            @RequestParam(required = false) LocalDate from,

            @RequestParam(required = false) LocalDate to

    ) {

        return ApiResponse.success(weightRecordService.getWeightRecords(userId, from, to));

    }



        @DeleteMapping("/{recordDate}")



        public ApiResponse<Void> deleteWeight(



                @PathVariable Long userId,



                @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordDate



        ) {



            weightRecordService.deleteWeightRecord(userId, recordDate);



            return ApiResponse.success("기록이 삭제되었습니다.", null);



        }



    }



    
