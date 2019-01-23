/*
 * Copyright 2019 Nordstrom, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nordstrom.kafka.connect.sqs ;

import java.util.Optional ;

import org.slf4j.Logger ;
import org.slf4j.LoggerFactory ;

/**
 * General utility test methods.
 *
 */
public class Facility {
  private static final Logger LOG = LoggerFactory.getLogger( Facility.class ) ;

  public static boolean isNotNullNorEmpty( String string ) {
    return string != null && !string.isEmpty() ;
  }

  public static boolean isNotNull( Object argument ) {
    return argument != null ;
  }

  public static boolean isInRange( int argument, int beginningInclusive, int endInclusive ) {
    if ( argument < beginningInclusive || argument > endInclusive ) {
      return false ;
    }
    return true ;
  }

  public static Optional<String> optionalOfEmptyOrNullable( String string ) {
    return isNotNullNorEmpty( string ) ? Optional.of( string ) : Optional.empty() ;
  }

  public static boolean isNotNullAndInRange( Integer argument, int beginningInclusive, int endInclusive ) {
    return isNotNull( argument ) && isInRange( argument, beginningInclusive, endInclusive ) ;
  }

  public static Integer parseInt( String value, String argumentName ) {
    try {
      return Integer.parseInt( value ) ;
    } catch ( NumberFormatException ex ) {
      LOG.info( String.format( "Unable to parse the value %1$s for the argument %2$s.", value, argumentName ) ) ;

      throw ex ;
    }
  }

}
