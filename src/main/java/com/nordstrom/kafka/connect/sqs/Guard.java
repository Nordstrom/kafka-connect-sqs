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

import java.net.MalformedURLException ;
import java.net.URISyntaxException ;
import java.net.URL ;

/**
 * Guard methods throw an exception should the test fail.
 *
 */
public class Guard {

  public static void verifyNotNullOrEmpty( String string, String argumentName ) {
    if ( !Facility.isNotNullNorEmpty( string ) ) {
      throw new IllegalArgumentException(
          String.format( "The argument %1$s should not be null or empty", argumentName ) ) ;
    }
  }

  public static void verifyValidUrl( String url ) {
    verifyNotNullOrEmpty( url, "url" ) ;
    try {
      URL u = new URL( url ) ; // this would check for the protocol
      u.toURI() ;
    } catch ( URISyntaxException | MalformedURLException e ) {
      throw new IllegalArgumentException( String.format( "Invalid Url: %1$s", url ), e ) ;
    }
  }

  public static void verifyNotNull( Object argument, String argumentName ) {
    if ( !Facility.isNotNull( argument ) ) {
      throw new IllegalArgumentException( String.format( "The argument %1$s should not be null", argumentName ) ) ;
    }
  }

  public static void verifyNonNegative( int argument, String argumentName ) {
    if ( argument < 0 ) {
      throw new IllegalArgumentException( String.format( "The argument %1$s should not be negative", argumentName ) ) ;
    }
  }

  public static void verifyInRange( int argument, int beginningInclusive, int endInclusive, String argumentName ) {
    if ( !Facility.isInRange( argument, beginningInclusive, endInclusive ) ) {
      throw new IllegalArgumentException(
          String.format( "The argument %1$s with value %2$s not in range ( %3$s , %4$s )", argumentName, argument,
              beginningInclusive, endInclusive ) ) ;
    }
  }

}
