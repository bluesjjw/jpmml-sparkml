/*
 * Copyright (c) 2016 Villu Ruusmann
 *
 * This file is part of JPMML-SparkML
 *
 * JPMML-SparkML is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-SparkML is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-SparkML.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.sparkml.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.spark.ml.classification.LogisticRegressionModel;
import org.apache.spark.ml.linalg.Matrix;
import org.apache.spark.ml.linalg.Vector;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.regression.RegressionModel;
import org.dmg.pmml.regression.RegressionTable;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.Feature;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.regression.RegressionModelUtil;
import org.jpmml.sparkml.ClassificationModelConverter;
import org.jpmml.sparkml.MatrixUtil;
import org.jpmml.sparkml.VectorUtil;

public class LogisticRegressionModelConverter extends ClassificationModelConverter<LogisticRegressionModel> implements HasRegressionOptions {

	public LogisticRegressionModelConverter(LogisticRegressionModel model){
		super(model);
	}

	@Override
	public RegressionModel encodeModel(Schema schema){
		LogisticRegressionModel model = getTransformer();

		CategoricalLabel categoricalLabel = (CategoricalLabel)schema.getLabel();

		if(categoricalLabel.size() == 2){
			List<Feature> features = new ArrayList<>(schema.getFeatures());
			List<Double> coefficients = new ArrayList<>(VectorUtil.toList(model.coefficients()));

			RegressionTableUtil.simplify(this, null, features, coefficients);

			RegressionModel regressionModel = RegressionModelUtil.createBinaryLogisticClassification(features, coefficients, model.intercept(), RegressionModel.NormalizationMethod.LOGIT, true, schema)
				.setOutput(null);

			return regressionModel;
		} else

		if(categoricalLabel.size() > 2){
			Matrix coefficientMatrix = model.coefficientMatrix();
			Vector interceptVector = model.interceptVector();

			List<RegressionTable> regressionTables = new ArrayList<>();

			for(int i = 0; i < categoricalLabel.size(); i++){
				String targetCategory = categoricalLabel.getValue(i);

				List<Feature> features = new ArrayList<>(schema.getFeatures());
				List<Double> coefficients = new ArrayList<>(MatrixUtil.getRow(coefficientMatrix, i));

				RegressionTableUtil.simplify(this, targetCategory, features, coefficients);

				RegressionTable regressionTable = RegressionModelUtil.createRegressionTable(features, coefficients, interceptVector.apply(i))
					.setTargetCategory(targetCategory);

				regressionTables.add(regressionTable);
			}

			RegressionModel regressionModel = new RegressionModel(MiningFunction.CLASSIFICATION, ModelUtil.createMiningSchema(categoricalLabel), regressionTables)
				.setNormalizationMethod(RegressionModel.NormalizationMethod.SOFTMAX);

			return regressionModel;
		} else

		{
			throw new IllegalArgumentException();
		}
	}
}