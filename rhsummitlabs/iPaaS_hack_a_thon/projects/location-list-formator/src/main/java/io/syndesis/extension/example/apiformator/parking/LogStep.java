/*
 * Copyright (C) 2016 Red Hat, Inc.
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
package io.syndesis.extension.example.apiformator.parking;

import java.util.Map;
import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.syndesis.extension.api.Step;
import io.syndesis.extension.api.annotations.Action;
import io.syndesis.extension.api.annotations.ConfigurationProperty;

@Action(id = "parking-location-format", name = "parking-location-format", description = "API Locations Formator - Parking")
public class LogStep implements Step{
    private static final Logger LOGGER = LoggerFactory.getLogger(LogStep.class);

    // ************************
    // Extension Properties
    // ************************

    @ConfigurationProperty(
    		name = "locationtype", 
    		displayName = "API Locations Type", 
    		description = "Types of location data",
    		type = "string" ,
    		enums= {@ConfigurationProperty.PropertyEnum(label=Formator.PARKING, value=Formator.PARKING ),
    				@ConfigurationProperty.PropertyEnum(label=Formator.ATM, value=Formator.ATM ),
    				@ConfigurationProperty.PropertyEnum(label=Formator.BAR, value=Formator.BAR ),
    				@ConfigurationProperty.PropertyEnum(label=Formator.STORE, value=Formator.STORE),
    				@ConfigurationProperty.PropertyEnum(label=Formator.RESTARUANT, value=Formator.RESTARUANT ),
    				},
            required = true
    )
 
    private String locationtype;
    
    

	public String getLocationtype() {
		return locationtype;
	}



	public void setLocationtype(String locationtype) {
		this.locationtype = locationtype;
	}



	@Override
	public Optional<ProcessorDefinition> configure(CamelContext context, ProcessorDefinition route,
			Map<String, Object> parameters) {
		// TODO Auto-generated method stub
		System.out.println("locationtype->["+locationtype+"]");
		Formator formator = new Formator();
		//
		return Optional.of(route.convertBodyTo(java.lang.String.class).bean(formator, "parseList(\""+locationtype+"\",${body})").marshal().json(JsonLibrary.Jackson).convertBodyTo(java.lang.String.class));
	}
}