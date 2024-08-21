package org.shaman.pmml;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.manager.PMMLManager;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.xml.sax.InputSource;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import javax.xml.transform.sax.SAXSource;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Johan Kaers
 */
public class EvaluateTest
{
    /*
    Train Decision Tree on Iris dataset in R using:
    > library("pmml")
    > library("rpart")
    > data("iris")
    > iris.rpart = rpart(Species ~ ., iris)
    > saveXML(pmml(iris.rpart), file = "~/SoftDev/R/iris_decisiontree.pmml")
     */
    
    public void run() throws Exception
    {
        // Load the PMML DecisionTree model generated in R for the Iris dataset
        InputStream pmmlStream = new FileInputStream("./src/main/resources/data/iris_decisiontree.pmml");
        InputSource pmmlSource = new InputSource(pmmlStream);
        SAXSource transformedSource = ImportFilter.apply(pmmlSource);
        PMML pmml = JAXBUtil.unmarshalPMML(transformedSource);
        PMMLManager pmmlManager = new PMMLManager(pmml);
        ModelEvaluator modelEvaluator = (ModelEvaluator)pmmlManager.getModelManager(ModelEvaluatorFactory.getInstance());
        Evaluator evaluator = (Evaluator)modelEvaluator;

        // Load the WEKA file containing the same Iris dataset
        DataSource irisSource = new DataSource("./src/main/resources/data/iris.arff");
        Instances irisInstances = irisSource.getDataSet();

        List<FieldName> activeFields = evaluator.getActiveFields();
        List<FieldName> targetFields = evaluator.getTargetFields();
        List<FieldName> outputFields = evaluator.getOutputFields();

        Map<FieldName, FieldValue> args;

        // For all instances in the dataset
        for(Instance instance: irisInstances)
        {
            // Convert them from WEKA instance to JPMML Evaluator Map
            int cntField = 0;
            args = new LinkedHashMap<FieldName, FieldValue>();
            for(FieldName activeField : activeFields)
            {
                double value = instance.value(irisInstances.attribute(cntField++));
                FieldValue activeValue = evaluator.prepare(activeField, value);
                args.put(activeField, activeValue);
            }
            
            // Evaluate Decision Tree trained in R on the WEKA instance
            Map<FieldName, ?> results = evaluator.evaluate(args);
            
            // Log match / no match
            String predictedSpecies = (String)results.get(outputFields.get(0));
            String givenSpecies = instance.stringValue(cntField);
            if (!givenSpecies.contains(predictedSpecies))
            {
                System.err.println("Mismatch "+predictedSpecies+" != "+givenSpecies+" for "+instance.toString());
            }
            else System.out.println("Match "+predictedSpecies+" != "+givenSpecies+" for "+instance.toString());
        }
    }

    public static void main(String []args)
    {
        EvaluateTest test = new EvaluateTest();
        try
        {
            test.run();
            System.exit(0);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();;
            System.exit(5);
        }
    }
}
