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

package com.starrocks.funcs

import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.types.Row

import scala.util.Random

/**
  * customised source
  */
class MySource extends SourceFunction[Row]{

  var isRunning = true

  override def run(ctx: SourceFunction.SourceContext[Row]) :Unit ={
    while (isRunning){
      val time = System.currentTimeMillis()
      val eleList = List("stephen","lebron","kobe")
      for(ele <- eleList){
        ctx.collect(Row.of(ele,Int.box(Random.nextInt(100))))
      }
      // 休眠 5s，发送下一次数据
      Thread.sleep(5000)
    }
  }

  override def cancel(): Unit = {
    isRunning = false
  }

}