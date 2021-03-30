/* Copyright 2002-2013 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.esa.orbiprotester.utils;

import org.apache.commons.math3.analysis.UnivariateVectorFunction;

/** Class used to {@link #integrate(UnivariateVectorFunction, double, double) integrate}
 *  a {@link org.apache.commons.math3.analysis.UnivariateVectorFunction function}
 *  of the orbital elements using the Gaussian quadrature rule to get the acceleration.
 *
 *  <p>
 *  This class uses only a 48 point gaussian quadrature.
 *  </p>
 *
 */
public class GaussQuadrature {

    /** Points for quadrature of order 48. */
    private static final double[] P_48 = {
        -0.99877100725242610000,
        -0.99353017226635080000,
        -0.98412458372282700000,
        -0.97059159254624720000,
        -0.95298770316043080000,
        -0.93138669070655440000,
        -0.90587913671556960000,
        -0.87657202027424800000,
        -0.84358826162439350000,
        -0.80706620402944250000,
        -0.76715903251574020000,
        -0.72403413092381470000,
        -0.67787237963266400000,
        -0.62886739677651370000,
        -0.57722472608397270000,
        -0.52316097472223300000,
        -0.46690290475095840000,
        -0.40868648199071680000,
        -0.34875588629216070000,
        -0.28736248735545555000,
        -0.22476379039468908000,
        -0.16122235606889174000,
        -0.09700469920946270000,
        -0.03238017096286937000,
        0.03238017096286937000,
        0.09700469920946270000,
        0.16122235606889174000,
        0.22476379039468908000,
        0.28736248735545555000,
        0.34875588629216070000,
        0.40868648199071680000,
        0.46690290475095840000,
        0.52316097472223300000,
        0.57722472608397270000,
        0.62886739677651370000,
        0.67787237963266400000,
        0.72403413092381470000,
        0.76715903251574020000,
        0.80706620402944250000,
        0.84358826162439350000,
        0.87657202027424800000,
        0.90587913671556960000,
        0.93138669070655440000,
        0.95298770316043080000,
        0.97059159254624720000,
        0.98412458372282700000,
        0.99353017226635080000,
        0.99877100725242610000
    };

    /** Weights for quadrature of order 48. */
    private static final double[] W_48 = {
        0.00315334605230596250,
        0.00732755390127620800,
        0.01147723457923446900,
        0.01557931572294386600,
        0.01961616045735556700,
        0.02357076083932435600,
        0.02742650970835688000,
        0.03116722783279807000,
        0.03477722256477045000,
        0.03824135106583080600,
        0.04154508294346483000,
        0.04467456085669424000,
        0.04761665849249054000,
        0.05035903555385448000,
        0.05289018948519365000,
        0.05519950369998416500,
        0.05727729210040315000,
        0.05911483969839566000,
        0.06070443916589384000,
        0.06203942315989268000,
        0.06311419228625403000,
        0.06392423858464817000,
        0.06446616443595010000,
        0.06473769681268386000,
        0.06473769681268386000,
        0.06446616443595010000,
        0.06392423858464817000,
        0.06311419228625403000,
        0.06203942315989268000,
        0.06070443916589384000,
        0.05911483969839566000,
        0.05727729210040315000,
        0.05519950369998416500,
        0.05289018948519365000,
        0.05035903555385448000,
        0.04761665849249054000,
        0.04467456085669424000,
        0.04154508294346483000,
        0.03824135106583080600,
        0.03477722256477045000,
        0.03116722783279807000,
        0.02742650970835688000,
        0.02357076083932435600,
        0.01961616045735556700,
        0.01557931572294386600,
        0.01147723457923446900,
        0.00732755390127620800,
        0.00315334605230596250
    };

    /** Node points. */
    private final double[] nodePoints;

    /** Node weights. */
    private final double[] nodeWeights;

    /** Creates a Gauss integrator of the given order.
     */
    public GaussQuadrature() {
        this.nodePoints  = P_48.clone();
        this.nodeWeights = W_48.clone();
    }

    /** Integrates a given function on the given interval.
     *
     *  @param f Function to integrate.
     *  @param lowerBound Lower bound of the integration interval.
     *  @param upperBound Upper bound of the integration interval.
     *  @return the integral of the weighted function.
     */
    public double[] integrate(final UnivariateVectorFunction f,
            final double lowerBound, final double upperBound) {

        final double[] adaptedPoints  = nodePoints.clone();
        final double[] adaptedWeights = nodeWeights.clone();
        transform(adaptedPoints, adaptedWeights, lowerBound, upperBound);
        return basicIntegrate(f, adaptedPoints, adaptedWeights);
    }

    /** Performs a change of variable so that the integration
     *  can be performed on an arbitrary interval {@code [a, b]}.
     *  <p>
     *  It is assumed that the natural interval is {@code [-1, 1]}.
     *  </p>
     *
     * @param points  Points to adapt to the new interval.
     * @param weights Weights to adapt to the new interval.
     * @param a Lower bound of the integration interval.
     * @param b Lower bound of the integration interval.
     */
    private void transform(final double[] points, final double[] weights,
            final double a, final double b) {
        // Scaling
        final double scale = (b - a) / 2;
        final double shift = a + scale;
        for (int i = 0; i < points.length; i++) {
            points[i]   = points[i] * scale + shift;
            weights[i] *= scale;
        }
    }

    /** Returns an estimate of the integral of {@code f(x) * w(x)},
     *  where {@code w} is a weight function that depends on the actual
     *  flavor of the Gauss integration scheme.
     *
     * @param f Function to integrate.
     * @param points  Nodes.
     * @param weights Nodes weights.
     * @return the integral of the weighted function.
     */
    private double[] basicIntegrate(final UnivariateVectorFunction f,
            final double[] points,
            final double[] weights) {
        double x = points[0];
        double w = weights[0];
        double[] v = f.value(x);
        final double[] y = new double[v.length];
        for (int j = 0; j < v.length; j++) {
            y[j] = w * v[j];
        }
        final double[] t = y.clone();
        final double[] c = new double[v.length];
        final double[] s = t.clone();
        for (int i = 1; i < points.length; i++) {
            x = points[i];
            w = weights[i];
            v = f.value(x);
            for (int j = 0; j < v.length; j++) {
                y[j] = w * v[j] - c[j];
                t[j] =  s[j] + y[j];
                c[j] = (t[j] - s[j]) - y[j];
                s[j] = t[j];
            }
        }
        return s;
    }
}
