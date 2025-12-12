package com.dietcoach.project.mapper;

import com.dietcoach.project.domain.WeightRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WeightRecordMapper {

    int insertWeightRecord(WeightRecord record);

    int updateWeightRecord(WeightRecord record);

    WeightRecord findByUserIdAndDate(@Param("userId") Long userId,
                                     @Param("recordDate") LocalDate recordDate);

    List<WeightRecord> findByUserIdBetweenDates(@Param("userId") Long userId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    WeightRecord findLatestByUserId(@Param("userId") Long userId);

    WeightRecord findByUserIdAndDateOnOrBefore(@Param("userId") Long userId,
                                               @Param("recordDate") LocalDate recordDate);
}
