package fr.rhiobet.benchmarks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class BenchStreamIO {

  @Param({"100000000"})
  private int N;
    
  private Path path;

  @Setup
  public void setup() throws Exception {
    path = Paths.get("data.txt");

    // Data generation
    PrintWriter writer = new PrintWriter(path.toFile());
    Random random = new Random();
    for (int i = 0; i < N; i++) {
      writer.println(random.nextInt(1000));
    }
    writer.close();
  }

  @Benchmark
  public int computeWithFor() throws Exception {
    List<Long> result = new ArrayList<>();    
    BufferedReader reader = new BufferedReader(new FileReader(path.toFile()));
    long temp;

    for (int i = 0; i < N; i++) {
      temp = Long.parseLong(reader.readLine());
      if (temp > 900) {
        result.add(temp);
      }
    }
    
    reader.close();

    return result.size();
  }

  @Benchmark
  public int computeWithStream() throws Exception {
    return Files.lines(path).mapToLong(Long::parseLong).filter(e -> e > 900).boxed().collect(Collectors.toList()).size();
  }

  @Benchmark
  public int computeWithParallelStream() throws Exception {
    return Files.lines(path).parallel().mapToLong(Long::parseLong).filter(e -> e > 900).boxed().collect(Collectors.toList()).size();
  }

  @Benchmark
  public int computeWithIterator() throws Exception {
    String line;
    long temp;
    BufferedReader reader = new BufferedReader(new FileReader(path.toFile()));
    List<Long> result = new ArrayList<>();

    while ((line = reader.readLine()) != null) {
      if ((temp = Long.parseLong(line)) > 900) {
        result.add(temp);
      }
    }
    
    reader.close();

    return result.size();
  }

}
