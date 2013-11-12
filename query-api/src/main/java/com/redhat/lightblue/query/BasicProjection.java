/*
    Copyright 2013 Red Hat, Inc. and/or its affiliates.

    This file is part of lightblue.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.redhat.lightblue.query;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.redhat.lightblue.util.Path;
import com.redhat.lightblue.util.Error;

public abstract class BasicProjection extends Projection {

    public static final String INVALID_PROJECTION="INVALID_PROJECTION";
    public static final String INVALID_ARRAY_RANGE_PROJECTION="INVALID_ARRAY_RANGE_PROJECTION";

    public static BasicProjection fromJson(ObjectNode node) {
        String field=node.get("field").asText();
        if(field==null)
            throw Error.get(INVALID_PROJECTION,"field");
        Path path=new Path(field);
        // Processing of optional elements. We decide on the type of
        // the final object based on what fields this object has
        JsonNode x=node.get("include");
        boolean include;
        if(x==null)
            include=true;
        else
            include=x.asBoolean();

        Projection projection;
        x=node.get("project");
        if(x==null) {
            // No projection. This is a field projection
            x=node.get("recursive");
            return new FieldProjection(path,include,
                                       x==null?false:x.asBoolean());
        }
        else {
            // Array projection
            projection=Projection.fromJson(x);
            x=node.get("match");
            if(x!=null) 
                return new ArrayQueryMatchProjection(path,
                                                     include,
                                                     projection,
                                                     QueryExpression.fromJson(x));
            else {
                x=node.get("range");
                if(x!=null) {
                    if(x instanceof ArrayNode &&
                       ((ArrayNode)x).size()==2) {
                        int from=((ArrayNode)x).get(0).asInt();
                        int to=((ArrayNode)x).get(1).asInt();
                        return new ArrayRangeProjection(path,
                                                        include,
                                                        projection,
                                                        from,to);
                    } else
                        throw Error.get(INVALID_ARRAY_RANGE_PROJECTION,node.toString());
                } else {
                    return new ArrayMatchingElementsProjection(path,
                                                               include,
                                                               projection);
                }
            }
        }
    }
}