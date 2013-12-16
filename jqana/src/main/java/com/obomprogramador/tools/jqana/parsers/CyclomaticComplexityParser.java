/**
 * jQana - Open Source Java(TM) code quality analyzer.
 * 
 * Copyright 2013 Cleuton Sampaio de Melo Jr
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Project website: http://www.jqana.com
 */
package com.obomprogramador.tools.jqana.parsers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.obomprogramador.tools.jqana.antlrparser.JavaLexer;
import com.obomprogramador.tools.jqana.antlrparser.JavaParser;
import com.obomprogramador.tools.jqana.context.Context;
import com.obomprogramador.tools.jqana.context.GlobalConstants;
import com.obomprogramador.tools.jqana.model.Measurement;
import com.obomprogramador.tools.jqana.model.Measurement.MEASUREMENT_TYPE;
import com.obomprogramador.tools.jqana.model.Metric;
import com.obomprogramador.tools.jqana.model.Parser;

import com.obomprogramador.tools.jqana.model.defaultimpl.DefaultMetric;
import com.obomprogramador.tools.jqana.model.defaultimpl.MaxLimitVerificationAlgorithm;
import com.obomprogramador.tools.jqana.model.defaultimpl.MetricValue;

import static com.obomprogramador.tools.jqana.context.GlobalConstants.*;

/**
 * Parser used to calculate cyclomatic complexity.
 * @see Parser
 * @author Cleuton Sampaio
 *
 */
public class CyclomaticComplexityParser implements Parser {


	protected Context context;
	protected Logger logger;
	protected Measurement packageMeasurement;
	protected Measurement measurement;
	protected Metric metric;
	protected MetricValue metricValue;
	
	/**
	 * 
	 */
	public CyclomaticComplexityParser(Measurement packageMeasurement,Context context) {
		super();
		logger = LoggerFactory.getLogger(this.getClass());
		this.context = context;
		this.packageMeasurement = packageMeasurement;
		this.metric = context.getCurrentMetric(context.getBundle().getString("metric.cc.name"));
		if (this.metric == null) {
			throw new IllegalArgumentException("Context is not valid. Metric is null.");
		}
	}

	/* (non-Javadoc)
	 * @see com.obomprogramador.tools.anacoja.model.Parser#getParserName()
	 */
	@Override
	public String getParserName() {
		return this.getClass().getName();
	}

	@Override
	public Measurement parse(Class<?> clazz, String sourceCode)  {
		
		this.measurement = new Measurement(); // Class name will be set inside listener.
		this.measurement.setType(MEASUREMENT_TYPE.CLASS_MEASUREMENT);
		this.metricValue = new MetricValue();
		this.metricValue.setName(this.metric.getMetricName());
		this.measurement.getMetricValues().add(this.metricValue);
		
		JavaLexer lexer;
		try {
			lexer = new JavaLexer(new ANTLRInputStream(sourceCode));
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			JavaParser p = new JavaParser(tokens);
	        ParseTree tree = (ParseTree)(p.compilationUnit()); 
	        ParseTreeWalker walker = new ParseTreeWalker();
	        CycloListener cl = new CycloListener(this.metric, this.measurement ,p);
	        walker.walk(cl, tree); 
	        // Compute the average:
	        MetricValue mv = this.measurement.getMetricValue(context.getBundle().getString("metric.cc.name"));
	        mv.setValue(mv.getValue() / mv.getQtdElements());
	        updatePackageMeasurement();
	        logger.debug("**** Complexidade: " + this.measurement.toString());
		} catch (Exception e) {
			logger.error("Parser: " + e.getMessage());
			context.getErrors().push(e.getMessage());
		}
            
		return this.measurement;
	}

	/* (non-javadoc
	 * We need to consolidate this class measurement into package measurement:
	 * 1 - Add this class' measruement to package's measurements collection;
	 * 2 - Add this class' metric value to package's metric values, calculating the average
	 * 3 - See if the metricvalue is violated
	 */
	private void updatePackageMeasurement() {
		
		Measurement classMeasurement = null;
		MetricValue mv = null;
		MetricValue packageMv = null;
		
		// 1 - Add ths chass' measurements to the package's measurements collection:
		
		int indx = this.packageMeasurement.getInnerMeasurements().indexOf(this.measurement);
		if (indx >= 0) {
			// Collection of inner measurements already has a measurement of this class. Ok.
			classMeasurement = this.packageMeasurement.getInnerMeasurements().get(indx);
		}
		else {
			// It is the first measurement of this class:
			classMeasurement = this.measurement;
			this.packageMeasurement.getInnerMeasurements().add(this.measurement);
		}
		
		// 2 - Add this class' metric value to package's metric values, calculating the average
		
		mv = classMeasurement.getMetricValue(context.getBundle().getString("metric.cc.name"));
		if (this.packageMeasurement.getMetricValues().contains(mv)) {
			// This package already contains a CC metric value, so, lets add to it:
			int mvIndx = this.packageMeasurement.getMetricValues().indexOf(mv);
			packageMv = this.packageMeasurement.getMetricValues().get(mvIndx);
			packageMv.setValue(packageMv.getValue() + mv.getValue());
			packageMv.setQtdElements(packageMv.getQtdElements() + 1);
			// Package average is calculated after all its classes.
			packageMv.setViolated(mv.isViolated());
		}
		else {
			packageMv = new MetricValue();
			packageMv.setName(mv.getName());
			packageMv.setValue(mv.getValue());
			packageMv.setQtdElements(1);
			packageMv.setViolated(mv.isViolated());
			this.packageMeasurement.getMetricValues().add(packageMv);
		}
	}
	


}
