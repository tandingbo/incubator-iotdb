/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.db.rest.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.apache.iotdb.db.conf.IoTDBConstant;
import org.apache.iotdb.db.metrics.MetricsSystem;
import org.apache.iotdb.db.rest.service.RestService;
import org.apache.iotdb.tsfile.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It’s used for mapping http request.
 */

@Path("/")
public class RestController {

  private static final Logger logger = LoggerFactory.getLogger(RestController.class);
  private RestService restService = RestService.getInstance();
  private MetricsSystem metricsSystem = new MetricsSystem();

  /**
   *
   * @param request this request will be in metricsJson format.
   * @return metricsJson in String
   */
  @Path("/query")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public JSONArray query(@Context HttpServletRequest request) {
    String targetStr = "target";
    try {
      JSONObject jsonObject = restService.getRequestBodyJson(request);
      assert jsonObject != null;
      JSONObject range = (JSONObject) jsonObject.get("range");
      Pair<String, String> timeRange = new Pair<>((String) range.get("from"), (String) range.get("to"));
      JSONArray array = (JSONArray) jsonObject.get("targets"); // metricsJson array is []
      JSONArray result = new JSONArray();
      for (int i = 0; i < array.size(); i++) {
        JSONObject object = (JSONObject) array.get(i);
        if (!object.containsKey(targetStr)) {
          result.add(JSONObject.parse("[]"));
          return result;
        }
        String target = (String) object.get(targetStr);
        String type = restService.getJsonType(jsonObject);
        JSONObject obj = new JSONObject();
        obj.put("target", target);
        if (type.equals("table")) {
          restService.setJsonTable(obj, target, timeRange);
        } else if (type.equals("timeserie")) {
          restService.setJsonTimeseries(obj, target, timeRange);
        }
        result.add(i, obj);
      }
      logger.info("query finished");
      return result;
    } catch (Exception e) {
      logger.error("/query failed", e);
    }
    return null;
  }

  /**
   * get metrics in json format
   */
  @PermitAll
  @Path("/metrics_information")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JSONObject getMetricsInformation() throws JsonProcessingException {
    return metricsSystem.metricsJson();
  }

  @PermitAll
  @Path("/server_information")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JSONObject getServerInformation() {
    return metricsSystem.serverJson();
  }

  /**
   * get sql argument
   */
  @PermitAll
  @Path("/sql_arguments")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public JSONArray getSqlArguments() {
    return metricsSystem.sqlJson();
  }

  @PermitAll
  @Path("/version")
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String getVersion() {
    return IoTDBConstant.VERSION;
  }
}
