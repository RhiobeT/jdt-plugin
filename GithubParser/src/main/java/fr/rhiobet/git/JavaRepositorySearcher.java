package fr.rhiobet.git;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.SearchRepository;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.RepositoryService;

/**
 * This class makes it possible to search for specific Java repositories, following some criteria, on GitHub
 */
public class JavaRepositorySearcher {
  
  private RepositoryService repositoryService;
  private ContentsService contentsService;
  private List<SearchRepository> results;
  private int maximumStars;
  
  
  /**
   * Constructs an instance that will make unauthenticated requests to the GitHub API.
   * <br />
   * Using this constructor is good enough for very basic operations, but the limit of requests is easily reached.
   * <br />
   * The maximum number of API calls using this constructor is 60 per hour.
   */
  public JavaRepositorySearcher() {
    this.repositoryService = new RepositoryService();
    this.contentsService = new ContentsService();
    this.results = new ArrayList<>();
    try {
      this.maximumStars = repositoryService.searchRepositories("language:java").get(0).getWatchers();
    } catch (IOException e) {
      e.printStackTrace();
      this.maximumStars = Integer.MAX_VALUE;
    }
  }
  
  
  /**
   * Constructs an instance that will make authenticated requests to the GitHub API (recommended).
   * <br />
   * The maximum number of API calls using this constructor is 5000 per hour.
   * @param client an AuthentifiedClient initialized with a valid GitHub API token
   */
  public JavaRepositorySearcher(AuthentifiedClient client) {
    this.repositoryService = new RepositoryService(client.getClient());
    this.contentsService = new ContentsService(client.getClient());
    this.results = new ArrayList<>();
    try {
      this.maximumStars = repositoryService.searchRepositories("language:java").get(0).getWatchers();
    } catch (IOException e) {
      e.printStackTrace();
      this.maximumStars = Integer.MAX_VALUE;
    }
  }
  
  
  /**
   * Returns the list of found repositories.
   * @return the list of found repositories
   */
  public List<Repository> getResults() {
    List<Repository> newResults = new ArrayList<>();
    for (SearchRepository repository : this.results) {
      newResults.add(new Repository(repository));
    }
    return newResults;
  }
 
  
  /**
   * Searches for a specific number of repositories, each having different stars rating.
   * 
   * Nothing is printed on the standard output.
   * @param number the wanted number of repositories
   * @param checkMaven whether the results should only include Maven repositories or not
   * @return the number of actually found repositories
   */ 
  public int findUsingStarsRepartition(int number, boolean checkMaven) {
    return this.findUsingStarsRepartition(number, checkMaven, false);
  }
  
  
  /**
   * Searches for a specific number of repositories, each having different stars rating.
   * @param number the wanted number of repositories
   * @param checkMaven whether the results should only include Maven repositories or not
   * @param showProgress whether to print the current progress on standard output or not
   * @return the number of actually found repositories
   */
  public int findUsingStarsRepartition(int number, boolean checkMaven, boolean showProgress) {
    int starsMax, starsMin, found;
    
    found = 0;
    starsMax = this.maximumStars;
  
    if (showProgress) {
      System.out.println("Starting search...");
    }
    while (found < number) {
      starsMin = starsMax * (number - found - 1) / (number - found);
      found += find(1, starsMin, starsMax, checkMaven);
      starsMax = starsMin - 1;
      if (showProgress) {
        System.out.println("Found: " + found + "/" + number);
      }
    }
    
    return found;
  }

  
  /**
   * This class is used to parse the results from search requests and extract the number of elements.   *
   */
  private static class RepositoryTotalCount implements Serializable {
    private static final long serialVersionUID = 5976656528144914397L;
    private int total_count;
    
    public int getTotalCount() {
      return this.total_count;
    }
  }
  
  
  /**
   * Gets the total number of Java repositories currently on GitHub. 
   * @return the total number of Java repositories currently on GitHub
   */
  public int getNumberOfRepositories() {
    return this.getNumberOfRepositories(0, this.maximumStars);
  }
  
  
  /**
   * Gets the total number of Java repositories on GitHub for a specific stars rating interval.
   * @param starsMin the minimum of the interval
   * @param starsMax the maximum of the interval
   * @return the number of corresponding Java repositories
   */
  public int getNumberOfRepositories(int starsMin, int starsMax) {
    GitHubRequest request = new GitHubRequest()
        .setUri("/search/repositories?q=language:java%20stars:" + starsMin + ".." + starsMax)
        .setType(RepositoryTotalCount.class);
    try {
      return ((RepositoryTotalCount) this.repositoryService.getClient().get(request).getBody()).getTotalCount();
    } catch (IOException e) {
      e.printStackTrace();
      return -1;
    }
  }
  
  
  /**
   * Searches for a specific number of repositories
   * @param number the wanted number of repositories
   * @param checkMaven whether the results should only include Maven repositories or not
   * @return the number of actually found repositories
   */
  public int find(int number, boolean checkMaven) {
    return this.find(number, 0, this.maximumStars, checkMaven);
  }
  
  
  /**
   * Searches for a specific number of repositories, according to the specified stars rating interval.
   * @param number the wanted number of repositories
   * @param starsMin the minimum stars rating for the wanted repositories
   * @param starsMax the maximum stars rating for the wanted repositories
   * @param checkMaven whether the results should only include Maven repositories or not
   * @return the number of actually found repositories
   */
  public int find(int number, int starsMin, int starsMax, boolean checkMaven) {      
    int pageIndex = 0, found = 0;
    boolean lastPage = false;
    
    try { 
      while (found < number) {
        List<SearchRepository> searchResults = repositoryService.searchRepositories(
            "language:java stars:" + starsMin + ".." + starsMax + " sort:updated", pageIndex);
        if (searchResults.size() < 100) {
          lastPage = true;
        }
        searchResults.removeAll(this.results);
        
        if (checkMaven) {
          this.filterMaven(searchResults, number - found);
        }
        
        if (number - found <= searchResults.size()) {
          this.results.addAll(searchResults.subList(0, number - found));
          found = number;
        } else {
          this.results.addAll(searchResults);
          found += searchResults.size();
          pageIndex++;
        }
        
        if (lastPage) {
          break;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return found;
  }

  
  /**
   * Filters the current results to only include the ones corresponding to Maven repositories.
   * 
   * All the others will be removed.
   */
  public void filterMaven() {
    this.filterMaven(this.results);
  }
  
  
  private void filterMaven(List<SearchRepository> repositories) {
    this.filterMaven(repositories, -1);
  }
  
  
  private void filterMaven(List<SearchRepository> repositories, int wantedNumber) {
    try {
      for (int i = 0; i < repositories.size(); i++) {
        if (wantedNumber == 0) {
          break;
        }
        boolean found = false;
        for (RepositoryContents content : this.contentsService.getContents(repositories.get(i))) {
          if (content.getName().equals("pom.xml")) {
            found = true;
            if (wantedNumber > 0) {
              wantedNumber--;
            }
            break;
          }
        }
        if (!found) {
          repositories.remove(i);
          i--;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
