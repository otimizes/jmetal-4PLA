//  AbYSSRunner.java
//
//  Author:
//       Antonio J. Nebro <antonio@lcc.uma.es>
//       Juan J. Durillo <durillo@lcc.uma.es>
//
//  Copyright (c) 2011 Antonio J. Nebro, Juan J. Durillo
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
package org.uma.jmetal.runner.multiobjective;

import org.uma.jmetal.core.Algorithm;
import org.uma.jmetal.core.Operator;
import org.uma.jmetal.core.Problem;
import org.uma.jmetal.core.SolutionSet;
import org.uma.jmetal.metaheuristic.multiobjective.abyss.AbYSS;
import org.uma.jmetal.operator.crossover.CrossoverFactory;
import org.uma.jmetal.operator.localSearch.MutationLocalSearch;
import org.uma.jmetal.operator.mutation.MutationFactory;
import org.uma.jmetal.problem.ProblemFactory;
import org.uma.jmetal.problem.zdt.ZDT4;
import org.uma.jmetal.qualityIndicator.QualityIndicatorGetter;
import org.uma.jmetal.util.JMetalException;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

/**
 * This class is the main program used to configure and run AbYSS, a
 * multiobjective scatter search metaheuristic, which is described in:
 * A.J. Nebro, F. Luna, E. Alba, B. Dorronsoro, J.J. Durillo, A. Beham
 * "AbYSS: Adapting Scatter Search to Multiobjective Optimization."
 * IEEE Transactions on Evolutionary Computation. Vol. 12,
 * No. 4 (August 2008), pp. 439-457
 */

public class AbYSSRunner {
  public static Logger LOGGER = Logger.getLogger(AbYSSRunner.class.getName());
  public static FileHandler fileHandler;

  /**
   * @param args Command line arguments.
   * @throws org.uma.jmetal.util.JMetalException
   * @throws IOException
   * @throws SecurityException Usage: three choices
   *                           - org.uma.jmetal.metaheuristic.multiobjective.nsgaII.NSGAII_main
   *                           - org.uma.jmetal.metaheuristic.multiobjective.nsgaII.NSGAII_main problemName
   *                           - org.uma.jmetal.metaheuristic.multiobjective.nsgaII.NSGAII_main problemName paretoFrontFile
   */
  public static void main(String[] args) throws
    JMetalException, SecurityException, IOException, ClassNotFoundException {
    Problem problem;
    Algorithm algorithm;
    Operator crossover;
    Operator mutation;
    Operator improvementOperator;

    QualityIndicatorGetter indicators;

    // Logger object and file to store log messages
    fileHandler = new FileHandler("AbYSS.log");
    LOGGER.addHandler(fileHandler);

    indicators = null;
    if (args.length == 1) {
      Object[] params = {"Real"};
      problem = (new ProblemFactory()).getProblem(args[0], params);
    } else if (args.length == 2) {
      Object[] params = {"Real"};
      problem = (new ProblemFactory()).getProblem(args[0], params);
      indicators = new QualityIndicatorGetter(problem, args[1]);
    } else { 
      //problem = new Kursawe("Real", 3);
      //problem = new Kursawe("BinaryReal", 3);
      //problem = new Water("Real");
      problem = new ZDT4("ArrayReal", 10);
      //problem = new ConstrEx("Real");
      //problem = new DTLZ1("Real");
      //problem = new OKA2("Real") ;
    } // else

    // STEP 2. Select the algorithm (AbYSS)
    algorithm = new AbYSS();
    algorithm.setProblem(problem);

    // STEP 3. Set the input parameters required by the metaheuristic
    algorithm.setInputParameter("populationSize", 20);
    algorithm.setInputParameter("refSet1Size", 10);
    algorithm.setInputParameter("refSet2Size", 10);
    algorithm.setInputParameter("archiveSize", 100);
    algorithm.setInputParameter("maxEvaluations", 25000);

    // STEP 4. Specify and configure the crossover operator, used in the
    //         solutiontype combination method of the scatter search
    HashMap<String, Object> crossoverParameters = new HashMap<String, Object>();
    crossoverParameters.put("probability", 0.9);
    crossoverParameters.put("distributionIndex", 20.0);
    crossover = CrossoverFactory.getCrossoverOperator("SBXCrossover", crossoverParameters);

    // STEP 5. Specify and configure the improvement method. We use by default
    //         a polynomial mutation in this method.
    HashMap<String, Object> mutationParameters = new HashMap<String, Object>();
    mutationParameters.put("probability", 1.0 / problem.getNumberOfVariables());
    mutationParameters.put("distributionIndex", 20.0);
    mutation = MutationFactory.getMutationOperator("PolynomialMutation", mutationParameters);

    HashMap<String, Object> parametersLocalSearch = new HashMap<String, Object>();
    parametersLocalSearch.put("improvementRounds", 1);
    parametersLocalSearch.put("problem", problem);
    parametersLocalSearch.put("mutation", mutation);
    improvementOperator = new MutationLocalSearch(parametersLocalSearch);

    // STEP 6. Add the operator to the algorithm
    algorithm.addOperator("crossover", crossover);
    algorithm.addOperator("improvement", improvementOperator);

    long initTime;
    long estimatedTime;
    initTime = System.currentTimeMillis();

    // STEP 7. Run the algorithm 
    SolutionSet population = algorithm.execute();
    estimatedTime = System.currentTimeMillis() - initTime;

    // STEP 8. Print the results
    LOGGER.info("Total execution time: " + estimatedTime + "ms");
    LOGGER.info("Variables values have been writen to file VAR");
    population.printVariablesToFile("VAR");
    LOGGER.info("Objectives values have been writen to file FUN");
    population.printObjectivesToFile("FUN");

    if (indicators != null) {
      LOGGER.info("Quality indicators");
      LOGGER.info("Hypervolume: " + indicators.getHypervolume(population));
      LOGGER.info("GD         : " + indicators.getGD(population));
      LOGGER.info("IGD        : " + indicators.getIGD(population));
      LOGGER.info("Spread     : " + indicators.getSpread(population));
      LOGGER.info("Epsilon    : " + indicators.getEpsilon(population));
    }
  }
}