// Copyright (c) 2021 Beijing Dingshi Zongheng Technology Co., Ltd. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// See the License for the specific language governing permissions and
// limitations under the License.

package com.starrocks.flink;

import com.starrocks.connector.flink.StarRocksSink;
import com.starrocks.connector.flink.table.StarRocksSinkOptions;
import com.starrocks.funcs.MySourceJava;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.types.Row;

import java.util.concurrent.TimeUnit;

/**
 * Demo2
 *   - sink json to StarRocks via flink-connector-starrocks
 */
public class Json2StarRocksJava {

  public static void main(String[] args) {
    StreamExecutionEnvironment env = getExecutionEnvironment();
    env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

    DataStream<String> source = env
      .addSource(new MySourceJava())
        .uid("sourceStream-uid").name("sourceStream")
        .setParallelism(1)
      .map(
        new MapFunction<Row, String>() {
          @Override
          public String map(Row value) throws Exception {
            String name = value.getField(0).toString();
            int score = Integer.parseInt(value.getField(1).toString());
            return "{\"name\": \"" + name + "\", \"score\": \"" + score + "\"}";    // map to json data
          }
        }
      )
        .uid("sourceStreamMap-uid").name("sourceStreamMap")
        .setParallelism(1);

    source
      .addSink(
        StarRocksSink.sink(
          /*
          The sink options for this demo:
          - hostname: master1
          - fe http port: 8030
          - database name: starrocks_demo
          - table names: demo2_flink_tb1
          - TODO: customize above args to fit your environment.
          */
          StarRocksSinkOptions.builder()
            .withProperty("jdbc-url", "jdbc:mysql://master1:9030/starrocks_demo")
            .withProperty("load-url", "master1:8030")
            .withProperty("username", "root")
            .withProperty("password", "")
            .withProperty("table-name", "demo2_flink_tb2")
            .withProperty("database-name", "starrocks_demo")
            .withProperty("sink.properties.format", "json")
            .withProperty("sink.properties.strip_outer_array", "true")
            .withProperty("sink.properties.row_delimiter","\\x02")      // in case of raw data contains common delimiter like '\n'
            .withProperty("sink.properties.column_separator","\\x01")   // in case of raw data contains common separator like '\t'
            .withProperty("sink.buffer-flush.interval-ms","5000")
            .build()
        )
      )
      .uid("sourceSink-uid").name("sourceSink")
      .setParallelism(1);

    try {
      env.execute("StarRocksSink_StringJava");
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private static StreamExecutionEnvironment getExecutionEnvironment(){
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    env.setMaxParallelism(3);
    env.setParallelism(3);
    env.setRestartStrategy(RestartStrategies.failureRateRestart(
            3, //failureRate
            org.apache.flink.api.common.time.Time.of(5, TimeUnit.MINUTES), // failureInterval
            org.apache.flink.api.common.time.Time.of(10, TimeUnit.SECONDS) // delayInterval
    ));
    // checkpoint options
    env.enableCheckpointing(1000 * 30);
    env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.AT_LEAST_ONCE);
    env.getCheckpointConfig().setMinPauseBetweenCheckpoints(500);
    env.getCheckpointConfig().setCheckpointTimeout(1000 * 60 * 10);
    env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
    env.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.DELETE_ON_CANCELLATION);
    env.getCheckpointConfig().setTolerableCheckpointFailureNumber(Integer.MAX_VALUE);
    return env;
  }

}
