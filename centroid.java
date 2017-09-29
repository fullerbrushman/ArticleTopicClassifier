import static java.nio.file.StandardOpenOption.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public class centroid {

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
    if(args.length > 8) {
      String[] options = Arrays.copyOfRange(args, 8, args.length);
    }

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

    // End Input Phase
    // Start Training

    for(int centroid_num = 0; centroid_num < numTopics; centroid_num++) {
      topics_centroids[centroid_num] = new Document(-1, attributeCount);
    }

    // normalize documents, then add to centroid and topic_counts
    for(int doc_num = 0; doc_num < trainDocumentCount; doc_num++) {
      Document doc = train_documents[doc_num];
      doc.normalize();
      topics_centroids[doc.getTopic()].add(doc);
      topics_counts[doc.getTopic()] += 1;
    }

    for(int doc_num = 0; doc_num < testDocumentCount; doc_num++) {
      test_documents[doc_num].normalize();
    }

    // for(int centroid_num = 0; centroid_num < numTopics; centroid_num++) {
    //   topics_centroids[centroid_num].scalarDivide(topics_counts[centroid_num]);
    // }

    // End Training

    // Start Testing

    for(int centroid_num = 0; centroid_num < numTopics; centroid_num++) {
      Document pos_centroid = topics_centroids[centroid_num].copyDoc();
      pos_centroid.scalarDivide(topics_counts[centroid_num]);
      Document neg_centroid = new Document(-1, attributeCount);
      int neg_centroid_count = 0;
      for(int centroid_num_2 = 0; centroid_num_2 < numTopics; centroid_num_2++) {
        if(centroid_num_2 != centroid_num) {
          neg_centroid.add(topics_centroids[centroid_num_2]);
          neg_centroid_count += topics_counts[centroid_num_2];
        }
      }
      neg_centroid.scalarDivide(neg_centroid_count);
      for(int doc_num = 0; doc_num < testDocumentCount; doc_num++) {
        Document doc = test_documents[doc_num];
        doc.addPlusVE(centroid_num, cosSimilarity(pos_centroid.attributes, doc.attributes));
        doc.addMinusVE(centroid_num, cosSimilarity(neg_centroid.attributes, doc.attributes));
      }
    }

    double[][] predictions = new double[20][testDocumentCount];

    for(int doc_num = 0; doc_num < testDocumentCount; doc_num++) {
      Document doc = test_documents[doc_num];
      doc.calculatePredictionScores();
      for(int centroid_num = 0; centroid_num < 20; centroid_num++) {
        predictions[centroid_num][doc_num] = doc.getPredictionScore(centroid_num);
      }
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
