import static java.nio.file.StandardOpenOption.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.Random;
import java.lang.Math;

public class regression {

  private static double dotProduct(double[] x, double[] y) {
    if (x.length != y.length) {
      return 0;
    }
    double accumulator = 0;
    for (int i = 0; i<x.length; i++) {
      accumulator += x[i] * y[i];
    }
    return accumulator;
  }

  private static double cosSimilarity(double[] x, double[] y) {
    if (x.length != y.length) {
      return 0;
    }
    double top = dotProduct(x, y);
    double bottom1 = Math.sqrt(dotProduct(x, x));
    double bottom2 = Math.sqrt(dotProduct(y, y));
    return dotProduct(x, y) / (Math.sqrt(dotProduct(x, x)) * Math.sqrt(dotProduct(y, y)));
  }

  private static double idf(int number_documents, int docs_contain) {
    if (docs_contain == 0){
      return 1;
    }
    return (Math.log((double) number_documents/(double) docs_contain) / Math.log(2.0));
  }

  private static double[] vectorSubtraction(double[] a, double[] b) {
    double[] returnVector = new double[a.length];
    for(int element_num = 0; element_num < a.length; element_num++) {
      returnVector[element_num] = a[element_num] - b[element_num];
    }
    return returnVector;
  }

  private static double[] vectorAddition(double[] a, double[] b) {
    double[] returnVector = new double[a.length];
    for(int element_num = 0; element_num < a.length; element_num++) {
      returnVector[element_num] = a[element_num] + b[element_num];
    }
    return returnVector;
  }

  private static double[] vectorMultiplication(double[] vector, double[] vector2) {
    double[] returnValue = new double[vector.length];
    for(int element_num = 0; element_num < vector.length; element_num++) {
      returnValue[element_num] = vector[element_num] * vector2[element_num];
    }
    return returnValue;
  }

  private static double[] generateYHat(Document[] x, double[] w, int i) {
    double[] returnVector = new double[x.length];
    for(int doc_num = 0; doc_num < x.length; doc_num++) {
      for(int column_num = 0; column_num < w.length; column_num++) {
        if(column_num != i) {
          returnVector[doc_num] += x[doc_num].attributes[column_num] * w[column_num];
        }
      }
    }
    return returnVector;
  }

  private static double generateError(double[] y, double[] yHat, double[] w, double lambda) {
    double[] thing = vectorSubtraction(yHat, y);
      return dotProduct(thing, thing) + lambda * dotProduct(w, w);
  }

  private static double winew(Document[] documents, double[] y, double[] wold, int i, double lambda) {
    double returnWiNew = 0.0;
    double[] xi = new double[documents.length];
    for(int doc_num = 0; doc_num < documents.length; doc_num++) {
      xi[doc_num] = documents[doc_num].getAttribute(i);
    }
    returnWiNew = dotProduct(xi, vectorSubtraction(y, generateYHat(documents, wold, i))) / (dotProduct(xi, xi) + lambda);
    if(returnWiNew < 0) {
      return 0.0;
    } else {
      return returnWiNew;
    }
  }

  public static int countAttributes(String filename) throws IOException {
  BufferedReader input = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(filename))));
  String current_line = input.readLine();
  String[] stuff = current_line.split(" ");
  int largest_attribute = Integer.parseInt(stuff[1]);
  while(current_line != null) {
    current_line = input.readLine();
      if(current_line != null) {
      stuff = current_line.split(" ");
      if (Integer.parseInt(stuff[1]) > largest_attribute) {
        largest_attribute = Integer.parseInt(stuff[1]);
      }
    }
  }
  input.close();
  return largest_attribute;
}

public static int countDocuments(String filename) throws IOException {
  BufferedReader input = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(filename))));
  String current_line = input.readLine();
  String[] stuff = current_line.split(" ");
  int current_doc = Integer.parseInt(stuff[0]);
  int countDocuments = 0;

  while(current_line != null) {
    current_line = input.readLine();
      if(current_line != null) {
      stuff = current_line.split(" ");
      if (Integer.parseInt(stuff[0]) != current_doc) {
        current_doc = Integer.parseInt(stuff[0]);
        countDocuments++;
      }
    }
  }
  input.close();
  return countDocuments;
}

  public static void main(String[] args) throws IOException {
    String input_file = args[0];
    String input_rlabel_file = args[1];
    String train_file = args[2];
    String test_file = args[3];
    String class_file = args[4];
    String feature_label_file = args[5];
    String feature_representation_option = args[6];
    String output_file = args[7];
    String[] options = Arrays.copyOfRange(args, 8, args.length);
    double[] lambdas = {0.01, 0.05, 0.1, 0.5, 1, 10};
    double lambda = -1.0;
    double best_lambda_f1 = 0.0;

    // Start Input Phase
    int numTopics = 20;  // Given in project description.

    Path input_path = Paths.get(input_file);
    InputStream input = Files.newInputStream(input_path);
    BufferedReader reader = new BufferedReader(new InputStreamReader(input));


    int attributeCount = countAttributes(input_file) + 1;
    int documentCount = countDocuments(input_file) + 1;


    /* Generate List of Documents */
    Document[] documents = new Document[documentCount];

    String current_line = reader.readLine();
    String[] stuff = current_line.split(" ");
    int current_index = Integer.parseInt(stuff[0]);
    Document current_doc = new Document(current_index, attributeCount);
    current_doc.addAttribute(Integer.parseInt(stuff[1]), Integer.parseInt(stuff[2]));
    int docs_added = 0;

    while(current_line != null) {
      current_line = reader.readLine();
      if(current_line != null) {
        stuff = current_line.split(" ");
        if (Integer.parseInt(stuff[0]) != current_index) {
          documents[docs_added] = current_doc;
          docs_added += 1;
          current_doc = new Document(Integer.parseInt(stuff[0]), attributeCount);
          if(feature_representation_option.equals("binary")) {
            current_doc.addAttribute(Integer.parseInt(stuff[1]), 1.0);
          } else {
            current_doc.addAttribute(Integer.parseInt(stuff[1]), (double) Integer.parseInt(stuff[2]));
          }
          /* Attempted Case Switch */
          // switch (feature_representation_option) {
          //   case "tf":      current_doc.addAttribute(Integer.parseInt(stuff[1]), (double) Integer.parseInt(stuff[2]));
          //                   break;
          //   case "binary":  current_doc.addAttribute(Integer.parseInt(stuff[1]), 1.0);
          //                   break;
          //   //case "tfidf":   current_doc.addAttribute(Integer.parseInt(stuff[1]), (double) Integer.parseInt(stuff[2]) * idf(documentCount, attributeOccurenceCount[Integer.parseInt(stuff[1])]));
          //   //                break;
          //   case "tfidf":   current_doc.addAttribute(Integer.parseInt(stuff[1]), (double) Integer.parseInt(stuff[2]));
          //                   break;
          // }
          current_index = Integer.parseInt(stuff[0]);
        } else {
          if(feature_representation_option.equals("binary")) {
            current_doc.addAttribute(Integer.parseInt(stuff[1]), 1.0);
          } else {
            current_doc.addAttribute(Integer.parseInt(stuff[1]), (double) Integer.parseInt(stuff[2]));
          }
          // switch (feature_representation_option) {
          //   case "tf":      current_doc.addAttribute(Integer.parseInt(stuff[1]), (double) Integer.parseInt(stuff[2]));
          //                   break;
          //   case "binary":  current_doc.addAttribute(Integer.parseInt(stuff[1]), 1.0);
          //                   break;
          //   //case "tfidf":   current_doc.addAttribute(Integer.parseInt(stuff[1]), (double) Integer.parseInt(stuff[2]) * idf(documentCount, attributeOccurenceCount[Integer.parseInt(stuff[1])]));
          //   //                break;
          //   case "tfidf":   current_doc.addAttribute(Integer.parseInt(stuff[1]), (double) Integer.parseInt(stuff[2]));
          //                   break;
          // }
        }
      }
    }
    documents[docs_added] = current_doc;

    /* Add Class and Doc ID to Documents */
    Path rlabel_path = Paths.get(input_rlabel_file);
    InputStream rlabel = Files.newInputStream(rlabel_path);
    BufferedReader rlabel_reader = new BufferedReader(new InputStreamReader(rlabel));

    String[] topics_list = new String[numTopics];  // filled now
    Document[] topics_centroids = new Document[numTopics];  // filled later
    int[] topics_counts = new int[numTopics];  // filled later

    current_line = rlabel_reader.readLine();
    stuff = current_line.split(" ");
    current_index = Integer.parseInt(stuff[0]);
    String current_topic = stuff[1];
    int current_doc_id = Integer.parseInt(stuff[2]);
    int topics_added = 0;
    topics_list[topics_added] = current_topic;
    topics_added++;

    while(current_line != null) {
      current_line = rlabel_reader.readLine();
      if(current_line != null) {
        stuff = current_line.split(" ");
        current_doc_id = Integer.parseInt(stuff[2]);
        documents[current_index - 1].setTopic(topics_added - 1);
        documents[current_index - 1].setRLabel(current_doc_id);
        current_index++;
        if(!stuff[1].equals(current_topic)) {
          current_topic = stuff[1];
          topics_list[topics_added] = current_topic;
          topics_added++;
        }
      }
    }

    // Start Selection of lambda

    Path lambda_train_path = Paths.get(options[0]);
    InputStream lambda_train = Files.newInputStream(lambda_train_path);
    BufferedReader lambda_train_reader = new BufferedReader(new InputStreamReader(lambda_train));

    int lambdaTrainDocumentCount = countDocuments(options[0]) + 1;

    /* Generate List of Lambda Train Documents */
    Document[] lambda_train_documents = new Document[lambdaTrainDocumentCount];

    current_line = lambda_train_reader.readLine();
    current_index = Integer.parseInt(current_line);
    current_doc = documents[current_index - 1];
    docs_added = 0;
    lambda_train_documents[docs_added] = current_doc;
    docs_added++;


    while(current_line != null) {
      current_line = lambda_train_reader.readLine();
      if(current_line != null) {
        current_index = Integer.parseInt(current_line);
        current_doc = documents[current_index - 1];
        lambda_train_documents[docs_added] = current_doc;
        docs_added++;
      }
    }

    if(feature_representation_option.equals("tfidf")) {
      int[] attributeOccurenceCount = new int[attributeCount];

      for(int train_doc_num = 0; train_doc_num < lambdaTrainDocumentCount; train_doc_num++) {
        for(int attribute_num = 0; attribute_num < attributeCount; attribute_num++) {
          if(lambda_train_documents[train_doc_num].getAttribute(attribute_num) != 0) {
            attributeOccurenceCount[attribute_num]++;
          }
        }
      }

      for(int doc_num = 0; doc_num < documentCount; doc_num++) {
        for(int attribute_num = 0; attribute_num < attributeCount; attribute_num++) {
          documents[doc_num].addAttribute(attribute_num, documents[doc_num].getAttribute(attribute_num) * idf(documentCount, attributeOccurenceCount[attribute_num]));
        }
      }
    }

    /* Generate List of Lambda Train Documents */
    Path lambda_test_path = Paths.get(options[1]);
    InputStream lambda_test = Files.newInputStream(lambda_test_path);
    BufferedReader lambda_test_reader = new BufferedReader(new InputStreamReader(lambda_test));

    int lambdaTestDocumentCount = countDocuments(options[1]) + 1; // make a document counter later.

    /* Generate List of Test Documents */
    Document[] lambda_test_documents = new Document[lambdaTestDocumentCount];

    current_line = lambda_test_reader.readLine();
    current_index = Integer.parseInt(current_line);
    current_doc = documents[current_index - 1];
    docs_added = 0;
    lambda_test_documents[docs_added] = current_doc;
    docs_added++;

    while(current_line != null) {
      current_line = lambda_test_reader.readLine();
      if(current_line != null) {
        current_index = Integer.parseInt(current_line);
        current_doc = documents[current_index - 1];
        lambda_test_documents[docs_added] = current_doc;
        docs_added += 1;
      }
    }

    // Start Training

    // normalize documents, then add to centroid and topic_counts
    for(int doc_num = 0; doc_num < lambdaTrainDocumentCount; doc_num++) {
      Document doc = lambda_train_documents[doc_num];
      doc.normalize();
      topics_counts[doc.getTopic()] += 1;
    }

    for(int doc_num = 0; doc_num < lambdaTestDocumentCount; doc_num++) {
      lambda_test_documents[doc_num].normalize();
    }

    for(int lambda_num = 0; lambda_num < lambdas.length; lambda_num++) {
      double current_lambda = lambdas[lambda_num];

      double[] y = new double[lambdaTrainDocumentCount];
      double[][] wList = new double[20][attributeCount];

      for(int topic_num = 0; topic_num < 20; topic_num++) {

        for(int doc_num = 0; doc_num < lambdaTrainDocumentCount; doc_num++) {
          if(lambda_train_documents[doc_num].getTopic() == topic_num) {
            y[doc_num] = 1;
          } else {
            y[doc_num] = 0;
          }
        }

        double[] wOld = new double[attributeCount];
        double[] wNew = new double[attributeCount];

        Random randoms = new Random(1);
        for(int attribute_num = 0; attribute_num < attributeCount; attribute_num++) {
          wNew[attribute_num] = randoms.nextDouble();
        }

        double[] yHatOld = new double[lambdaTrainDocumentCount];
        double[] yHatNew = generateYHat(lambda_train_documents, wNew, -1);

        do {
          yHatOld = yHatNew;
          wOld = wNew;
          wNew = new double[attributeCount];
          for(int element_num = 0; element_num < attributeCount; element_num++) {
            wNew[element_num] = winew(lambda_train_documents, y, wOld, element_num, current_lambda);
          }
          yHatNew = generateYHat(lambda_train_documents, wNew, -1);

        } while(Math.abs(generateError(y, yHatNew, wNew, current_lambda) - generateError(y, yHatOld, wOld, current_lambda)) > 0.001);

        wList[topic_num] = wNew;
      }

      double[][] predictions = new double[20][lambdaTestDocumentCount];

      for(int doc_num = 0; doc_num < lambdaTestDocumentCount; doc_num++) {
        Document doc = lambda_test_documents[doc_num];
        doc.calculatePredictionScores();
        for(int centroid_num = 0; centroid_num < 20; centroid_num++) {
          predictions[centroid_num][doc_num] = doc.getPredictionScore(centroid_num);
        }
      }

      // End Training

      // Start Testing

      for(int doc_num = 0; doc_num < lambdaTestDocumentCount; doc_num++) {
        Document doc = lambda_test_documents[doc_num];
        for(int topic_num = 0; topic_num < 20; topic_num++) {
          doc.setPredictionScore(topic_num, dotProduct(doc.attributes, wList[topic_num]));
        }
        doc.assignTopic();
      }

      /* Now need to get max F1Score for each class */
      double cummulativeF1Score = 0.0;
      /* check in descending order */
      for(int centroid_num = 0; centroid_num < 20; centroid_num++) {
        double maxf1score = 0;
        //for(int el = testDocumentCount - 1; el > -1; el--) {
        for(int el = 0; el < lambdaTestDocumentCount; el++) {
          double cutoff = predictions[centroid_num][el];
          int a = 0;
          int b = 0;
          int c = 0;
          for(int doc_num = 0; doc_num < lambdaTestDocumentCount; doc_num++) {
            // These are predicted of class centroid_num
            if(lambda_test_documents[doc_num].getPredictionScore(centroid_num) >= cutoff) {
              if(lambda_test_documents[doc_num].getTopic() == centroid_num) {
                a++;
              } else {
                c++;
              }
            } else {
              if(lambda_test_documents[doc_num].getTopic() == centroid_num) {
                b++;
              }
            }
          }
          // for(int doc_num = el; doc_num < testDocumentCount; doc_num++) {
          //   // These are predicted of not class centroid_num
          //   if(test_documents[doc_num].getTopic() == centroid_num) {
          //     b++;
          //   }
          // }
          double f1score = 0.0;
          double denominator = (2 * (double) a + (double) b + (double) c);
          if(denominator != 0) {
            f1score = 2 * (double) a / denominator;
          }
          if(f1score > maxf1score) {
            maxf1score += f1score;
          }
        }
        cummulativeF1Score += maxf1score;
      }
      // End Testing
      if(cummulativeF1Score / 20 > best_lambda_f1) {
        best_lambda_f1 = cummulativeF1Score / 20;
        lambda = current_lambda;
      }
    }
    System.out.println(Double.toString(lambda));
    // End Selection of lambda

    Path train_path = Paths.get(train_file);
    InputStream train = Files.newInputStream(train_path);
    BufferedReader train_reader = new BufferedReader(new InputStreamReader(train));

    int trainDocumentCount = countDocuments(train_file) + 1; // make a document counter later.

    /* Generate List of Train Documents */
    Document[] train_documents = new Document[trainDocumentCount];

    current_line = train_reader.readLine();
    current_index = Integer.parseInt(current_line);
    current_doc = documents[current_index - 1];
    docs_added = 0;
    train_documents[docs_added] = current_doc;
    docs_added++;


    while(current_line != null) {
      current_line = train_reader.readLine();
      if(current_line != null) {
        current_index = Integer.parseInt(current_line);
        current_doc = documents[current_index - 1];
        train_documents[docs_added] = current_doc;
        docs_added++;
      }
    }

    if(feature_representation_option.equals("tfidf")) {
      int[] attributeOccurenceCount = new int[attributeCount];

      for(int train_doc_num = 0; train_doc_num < trainDocumentCount; train_doc_num++) {
        for(int attribute_num = 0; attribute_num < attributeCount; attribute_num++) {
          if(train_documents[train_doc_num].getAttribute(attribute_num) != 0) {
            attributeOccurenceCount[attribute_num]++;
          }
        }
      }

      for(int doc_num = 0; doc_num < documentCount; doc_num++) {
        for(int attribute_num = 0; attribute_num < attributeCount; attribute_num++) {
          documents[doc_num].addAttribute(attribute_num, documents[doc_num].getAttribute(attribute_num) * idf(documentCount, attributeOccurenceCount[attribute_num]));
        }
      }
    }

    Path test_path = Paths.get(test_file);
    InputStream test = Files.newInputStream(test_path);
    BufferedReader test_reader = new BufferedReader(new InputStreamReader(test));

    int testDocumentCount = countDocuments(test_file) + 1; // make a document counter later.

    /* Generate List of Test Documents */
    Document[] test_documents = new Document[testDocumentCount];

    current_line = test_reader.readLine();
    current_index = Integer.parseInt(current_line);
    current_doc = documents[current_index - 1];
    docs_added = 0;
    test_documents[docs_added] = current_doc;
    docs_added++;

    while(current_line != null) {
      current_line = test_reader.readLine();
      if(current_line != null) {
        current_index = Integer.parseInt(current_line);
        current_doc = documents[current_index - 1];
        test_documents[docs_added] = current_doc;
        docs_added += 1;
      }
    }

    Path feature_label_path = Paths.get(feature_label_file);
    InputStream feature_label = Files.newInputStream(feature_label_path);
    BufferedReader feature_label_reader = new BufferedReader(new InputStreamReader(feature_label));

    /* Generate List of Feature Labels */
    String[] features = new String[attributeCount];

    current_line = test_reader.readLine();
    int features_added = 0;
    features[features_added] = current_line;
    features_added++;

    while(current_line != null) {
      current_line = test_reader.readLine();
      if(current_line != null) {
        features[features_added] = current_line;
        features_added += 1;
      }
    }

    // End Input Phase
    // Start Training

    // normalize documents, then add to centroid and topic_counts
    for(int doc_num = 0; doc_num < trainDocumentCount; doc_num++) {
      Document doc = train_documents[doc_num];
      doc.normalize();
      topics_counts[doc.getTopic()] += 1;
    }

    for(int doc_num = 0; doc_num < testDocumentCount; doc_num++) {
      test_documents[doc_num].normalize();
    }

    double[] y = new double[trainDocumentCount];
    double[][] wList = new double[20][attributeCount];

    for(int topic_num = 0; topic_num < 20; topic_num++) {

      for(int doc_num = 0; doc_num < trainDocumentCount; doc_num++) {
        if(train_documents[doc_num].getTopic() == topic_num) {
          y[doc_num] = 1;
        } else {
          y[doc_num] = 0;
        }
      }

      double[] wOld = new double[attributeCount];
      double[] wNew = new double[attributeCount];

      Random randoms = new Random(1);
      for(int attribute_num = 0; attribute_num < attributeCount; attribute_num++) {
        wNew[attribute_num] = randoms.nextDouble();
      }

      double[] yHatOld = new double[trainDocumentCount];
      double[] yHatNew = generateYHat(train_documents, wNew, -1);

      do {
        yHatOld = yHatNew;
        wOld = wNew;
        wNew = new double[attributeCount];
        for(int element_num = 0; element_num < attributeCount; element_num++) {
          wNew[element_num] = winew(train_documents, y, wOld, element_num, lambda);
        }
        yHatNew = generateYHat(train_documents, wNew, -1);

      } while(Math.abs(generateError(y, yHatNew, wNew, lambda) - generateError(y, yHatOld, wOld, lambda)) < 0.001);

      wList[topic_num] = wNew;
    }

    double[][] predictions = new double[20][lambdaTestDocumentCount];

    for(int doc_num = 0; doc_num < lambdaTestDocumentCount; doc_num++) {
      Document doc = lambda_test_documents[doc_num];
      doc.calculatePredictionScores();
      for(int centroid_num = 0; centroid_num < 20; centroid_num++) {
        predictions[centroid_num][doc_num] = doc.getPredictionScore(centroid_num);
      }
    }

    // End Training

    // Start Testing

    for(int doc_num = 0; doc_num < testDocumentCount; doc_num++) {
      Document doc = test_documents[doc_num];
      for(int topic_num = 0; topic_num < 20; topic_num++) {
        doc.setPredictionScore(topic_num, dotProduct(doc.attributes, wList[topic_num]));
      }
      doc.assignTopic();
    }

    // End Testing

    //output_file printing start

    File outfile = new File(output_file);
		if (!outfile.exists()) {
			outfile.createNewFile();
		}
		FileWriter filewriter = new FileWriter(outfile.getAbsoluteFile());
		BufferedWriter writter = new BufferedWriter(filewriter);

    for(int doc_num = 0; doc_num < testDocumentCount; doc_num++) {

      writter.write(Integer.toString(test_documents[doc_num].getRLabel()) + ", " + topics_list[test_documents[doc_num].getAssignedTopic()] + "\n");
    }

    writter.close();
    //output_file printing end

    // for(int centroid_num = 0; centroid_num < 20; centroid_num++) {
    //   Arrays.sort(predictions[centroid_num]);
    // }

    /* Now need to get max F1Score for each class */
    /* check in descending order */
    for(int centroid_num = 0; centroid_num < 20; centroid_num++) {
      double maxf1score = 0;
      //for(int el = testDocumentCount - 1; el > -1; el--) {
      for(int el = 0; el < testDocumentCount; el++) {
        double cutoff = predictions[centroid_num][el];
        int a = 0;
        int b = 0;
        int c = 0;
        for(int doc_num = 0; doc_num < testDocumentCount; doc_num++) {
          // These are predicted of class centroid_num
          if(test_documents[doc_num].getPredictionScore(centroid_num) >= cutoff) {
            if(test_documents[doc_num].getTopic() == centroid_num) {
              a++;
            } else {
              c++;
            }
          } else {
            if(test_documents[doc_num].getTopic() == centroid_num) {
              b++;
            }
          }
        }
        // for(int doc_num = el; doc_num < testDocumentCount; doc_num++) {
        //   // These are predicted of not class centroid_num
        //   if(test_documents[doc_num].getTopic() == centroid_num) {
        //     b++;
        //   }
        // }
        double f1score = 0.0;
        double denominator = (2 * (double) a + (double) b + (double) c);
        if(denominator != 0) {
          f1score = 2 * (double) a / denominator;
        }
        if(f1score > maxf1score) {
          maxf1score = f1score;
        }
      }
      System.out.println("MAXF1: " + topics_list[centroid_num] + " " + Double.toString(maxf1score));

      // Start Getting Top 10 Important Attributes

      double[] best10WList = new double[10];
      int[] locations = new int[10];

      for(int w_num = 0; w_num < attributeCount; w_num++) {
        double candidate = wList[centroid_num][w_num];
        int place = -1;
        for(int element_num = 0; element_num < 10; element_num++) {
          if(candidate < best10WList[element_num]) {
            candidate = best10WList[element_num];
            place = element_num;
          }
        }
        if(place >= 0) {
          best10WList[place] = wList[centroid_num][w_num];
          locations[place] = w_num;
        }
      }
      for(int w_num = 0; w_num < 10; w_num++) {
        System.out.println(features[locations[w_num]] + ": " + Double.toString(best10WList[w_num]));
      }

      // End Getting Top 10 Important Attributes

    }
    // End Testing

  }  // main

}  // centroid

class Document {
  public int i;
  public double[] attributes;
  public int topic;
  public boolean train;
  public int rlabel;
  public int assigned_topic;
  public double[] plusVE;
  public double[] minusVE;
  public double[] predictionscores;

  public Document(int i, int countAttributes) {
    this.i = i;
    this.attributes = new double[countAttributes];
    this.train = false;
    this.rlabel = -1;
    this.assigned_topic = -1;
    this.plusVE = new double[20];
    this.minusVE = new double[20];
    this.predictionscores = new double[20];
  }

  public void calculatePredictionScores() {
    double largest_prediction_score = -100;
    for(int i = 0; i < 20; i++) {
      this.predictionscores[i] = plusVE[i] - minusVE[i];
      if(this.predictionscores[i] > largest_prediction_score) {
        largest_prediction_score = predictionscores[i];
        this.assigned_topic = i;
      }
    }
  }

  public void assignTopic() {
    double largest_prediction_score = -100;
    for(int element_num = 0; element_num < 20; element_num++) {
      if(largest_prediction_score < predictionscores[element_num]) {
        this.assigned_topic = element_num;
      }
    }
  }

  public void setPredictionScore(int location, double value) {
    this.predictionscores[location] = value;
  }

  public double getPredictionScore(int location) {
    return this.predictionscores[location];
  }

  public int getAssignedTopic() {
    return this.assigned_topic;
  }

  public void addPlusVE(int location, double value) {
    this.plusVE[location] = value;
  }

  public void addMinusVE(int location, double value) {
    this.minusVE[location] = value;
  }

  public void setTrain(boolean value) {
    this.train = value;
  }

  public void setTopic(int topic) {
    this.topic = topic;
  }

  public int getTopic() {
    return this.topic;
  }

  public void addAttribute(int j, double k) {
    this.attributes[j] = k;
  }

  public double getAttribute(int attribute_num) {
    return this.attributes[attribute_num];
  }

  public void setRLabel(int rLabel) {
    this.rlabel = rLabel;
  }

  public int getRLabel() {
    return this.rlabel;
  }

  public Document copyDoc() {
    Document doc = new Document(this.i, this.attributes.length);
    for(int k = 0; k < attributes.length; k++) {
      doc.attributes[k] = this.attributes[k];
    }
    return doc;
  }

  public void normalize() {
    int count = 0;
    for(int k = 0; k < attributes.length; k++) {
      count += this.attributes[k];
    }
    for(int k = 0; k < attributes.length; k++) {
      this.attributes[k] = this.attributes[k]/(double) count;
    }
  }

  public boolean equals(Document doc) {
    if(this.attributes.length != doc.attributes.length) {
      return false;
    }
    for(int attribute_num = 0; attribute_num < attributes.length; attribute_num++) {
      if(this.attributes[attribute_num] != doc.getAttribute(attribute_num)){
        return false;
      }
    }
    return true;
  }

  public void add(Document doc) {
    for(int k = 0; k < attributes.length; k++) {
      this.attributes[k] += doc.getAttribute(k);
    }
  }

  public void scalarDivide(int scalar) {
    for(int k = 0; k < attributes.length; k++) {
      this.attributes[k] = this.attributes[k] / (double) scalar;
    }
  }

}
