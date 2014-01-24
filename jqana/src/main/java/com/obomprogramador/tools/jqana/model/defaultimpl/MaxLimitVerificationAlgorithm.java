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
package com.obomprogramador.tools.jqana.model.defaultimpl;

import com.obomprogramador.tools.jqana.model.LimitVerificationAlgorithm;

/**
 * Implementation of LimitVerificationAlgorothm that checks whether a metric value is greater
 * than a max limit.
 * @see LimitVerificationAlgorithm
 *
 * @author Cleuton Sampaio.
 *
 */
public class MaxLimitVerificationAlgorithm extends
		LimitVerificationAlgorithm {

	private double maxLimit;
	public MaxLimitVerificationAlgorithm(double maxLimit) {
		super();
		this.maxLimit = maxLimit;
	}
	
	public MaxLimitVerificationAlgorithm() {
		super();
	}
	
	

	public double getMaxLimit() {
		return maxLimit;
	}

	public void setMaxLimit(double maxLimit) {
		this.maxLimit = maxLimit;
	}

	/**
	 * Checks whether the metric's value is greater than the specified limit.
	 */
	@Override
	public boolean verify(double value) {
		boolean returnCode = false;
		if (value > maxLimit) {
			returnCode = true;
		}
		return returnCode;
	}

}