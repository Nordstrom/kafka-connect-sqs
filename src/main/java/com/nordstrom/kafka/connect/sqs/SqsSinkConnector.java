/*
 * Copyright 2019 Nordstrom, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.nordstrom.kafka.connect.sqs ;

import java.util.ArrayList ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Map ;

import org.apache.kafka.common.config.ConfigDef ;
import org.apache.kafka.connect.connector.Task ;
import org.apache.kafka.connect.sink.SinkConnector ;
import org.slf4j.Logger ;
import org.slf4j.LoggerFactory ;

import com.nordstrom.kafka.connect.About ;
import com.nordstrom.kafka.connect.sqs.SqsSinkConnectorConfig ;

public class SqsSinkConnector extends SinkConnector {
  private final Logger log = LoggerFactory.getLogger( this.getClass() ) ;

  private Map<String, String> configProps ;

  @Override
  public String version() {
    return About.CURRENT_VERSION ;
  }

  @Override
  public void start( Map<String, String> props ) {
    configProps = props ;
    log.info( "connector.start:OK" ) ;
  }

  @Override
  public Class<? extends Task> taskClass() {
    return SqsSinkConnectorTask.class ;
  }

  @Override
  public List<Map<String, String>> taskConfigs( int maxTasks ) {
    List<Map<String, String>> taskConfigs = new ArrayList<>( maxTasks ) ;
    Map<String, String> taskProps = new HashMap<>( configProps ) ;
    for ( int i = 0 ; i < maxTasks ; i++ ) {
      taskConfigs.add( taskProps ) ;
    }
    return taskConfigs ;
  }

  @Override
  public void stop() {
    log.info( "connector.stop:OK" ) ;
  }

  @Override
  public ConfigDef config() {
    return SqsSinkConnectorConfig.config() ;
  }

}
