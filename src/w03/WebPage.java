// Peter Idestam-Almquist, 2018-02-26.

// [Do NOT modify this file.]
package w03;

import java.util.Random;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.HashMap;

public class WebPage {
	private URI uri = null;
	private String content = null;
	private Map<String, Integer> wordMap = new HashMap<String, Integer>();
	private int maxCounter = 0;
	private String category = null;

	private static long sum = 0; // Only used to fake hard work.

	WebPage(String uriString) {
		try {
			uri = new URI(uriString);
		}
		catch (URISyntaxException exception) {
		}
	}
	
	void download() {
		Random random = new Random();
		int numWords = 1 + random.nextInt(10);
		String content = "";
		for (int i = 0; i < numWords; i++)
			content += "word" + (random.nextInt(5) + 1) + " ";
		this.content = content;
		// Fake wait for web-response.
		try {
			Thread.sleep(100);
		}
		catch (InterruptedException exception) {
			System.out.println(exception);
		}
	}

	private void countWord(String word) {
		if (wordMap.containsKey(word))
			wordMap.put(word, wordMap.get(word) + 1);
		else 
			wordMap.put(word, 1);
	}

	void analyze() {
		String[] words = content.split(" ");
		for (int i = 0; i < words.length; i++)
			countWord(words[i]);
		// Do some hard work.
		sum = 0;
		for (int i = 0; i < Integer.MAX_VALUE / 10; i++) {
			sum += i;
		}
	}
	
	private void storeMax(String word, int counter) {
		if (counter > maxCounter) {
			maxCounter = counter;
			category = word;
		}
	}
	
	void categorize() {
		wordMap.forEach((k, v) -> storeMax(k, v));
		// Do some hard work.
		sum = 0;
		for (int i = 0; i < Integer.MAX_VALUE / 20; i++) {
			sum += i;
		}
	}
	
	public String toString() {
		String tmp = "";
		if (uri != null)
			tmp += "uri = " + uri;
		if (category != null)
			tmp += "; category = " + category;
		return tmp;
	}
}
