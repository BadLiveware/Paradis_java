// Peter Idestam-Almquist, 2018-02-26.
// Fredrik Larsson - frla9839

// [Do necessary modifications of this file.]

package w03;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// [You are welcome to add some import statements.]

public class Program1 {
	final static int NUM_WEBPAGES = 40;
	private static WebPage[] webPages = new WebPage[NUM_WEBPAGES];
	// [You are welcome to add some variables.]
	private static int threads = Runtime.getRuntime().availableProcessors();
	private static ExecutorService tPool = Executors.newFixedThreadPool(threads);
	static BlockingQueue<WebPage> wpgs = new LinkedBlockingQueue<WebPage>();

	// [You are welcome to modify this method, but it should NOT be parallelized.]
	private static void initialize() {
		for (int i = 0; i < NUM_WEBPAGES; i++) {
			webPages[i] = new WebPage("http://www.site.se/page" + i + ".html");
			try {
				wpgs.put(webPages[i]);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// [You are welcome to modify this method, but it should NOT be parallelized.]
	private static void presentResult() {
		for (int i = 0; i < NUM_WEBPAGES; i++) {
			System.out.println(webPages[i]);
		}
	}

	public static void main(String[] args) {
		// Initialize the list of webpages.
		initialize();

		// Start timing.
		long start = System.nanoTime();

		// [Do modify this sequential part of the program.]
		// This is kind of cheating in a consumer producer way but since the actual work
		// is bounded this should be the most efficient way;
		// FIXME: Change this to be less cheaty?
		BlockingQueue<WebPage> downloaderDone = new LinkedBlockingQueue<>();
		BlockingQueue<WebPage> analyzerDone = new LinkedBlockingQueue<>();
		BlockingQueue<WebPage> categorizerDone = new LinkedBlockingQueue<>();
		
		List<Worker> workers = IntStream.range(0, threads).mapToObj(i -> {
			// Split the workers equally-ish
			Worker worker = null;
			switch (i % 3) {
			case 0:
				worker = new Downloader(wpgs, downloaderDone); break;
			case 1: 
				worker = new Analyzer(downloaderDone, analyzerDone); break;
			default:
				worker = new Categorizer(analyzerDone, categorizerDone); break;
			}
			tPool.submit(worker);
			return worker;
		}).collect(Collectors.toList());

		
		try {
			synchronized (categorizerDone) {
				while (categorizerDone.size() < NUM_WEBPAGES) {
					categorizerDone.wait();
				}
			}

			workers.forEach(worker -> {
				worker.stop();
			});
			
			tPool.shutdown();
			tPool.awaitTermination(5000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		// Stop timing.
		long stop = System.nanoTime();

		// Present the result.
		presentResult();

		// Present the execution time.
		System.out.println("Execution time (seconds): " + (stop - start) / 1.0E9);
	}

	static interface Worker extends Callable<Void> {		
		public void stop();
	}
	
	static class Downloader implements Worker {
		private BlockingQueue<WebPage> input;
		private BlockingQueue<WebPage> output;
		private volatile boolean running;
	
		public Downloader(BlockingQueue<WebPage> input, BlockingQueue<WebPage> output) {
			this.input = input;
			this.output = output;
		}

		@Override
		public Void call() throws Exception {
			running = true;
			while (running) {
				try {
					WebPage page = input.poll(50, TimeUnit.MILLISECONDS);
					if (page == null)
						break;
					page.download();
					output.put(page);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		public void stop() {
			running = false;
		}
	}
	
	static class Analyzer implements Worker {
		private BlockingQueue<WebPage> input;
		private BlockingQueue<WebPage> output;
		private volatile boolean running;
	
		public Analyzer(BlockingQueue<WebPage> input, BlockingQueue<WebPage> output) {
			this.input = input;
			this.output = output;
		}

		@Override
		public Void call() throws Exception {
			running = true;
			while (running) {
				try {
					WebPage page = input.poll(50, TimeUnit.MILLISECONDS);
					if (page == null) 
						continue;
					page.analyze();
					output.offer(page);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		public void stop() {
			running = false;
		}
	}
	
	static class Categorizer implements Worker {
		private BlockingQueue<WebPage> input;
		private BlockingQueue<WebPage> output;
		private volatile boolean running;
	
		public Categorizer(BlockingQueue<WebPage> input, BlockingQueue<WebPage> output) {
			this.input = input;
			this.output = output;
		}

		@Override
		public Void call() throws Exception {
			running = true;
			while (running) {
				if (output.size() == NUM_WEBPAGES) {
					synchronized (output) {
						output.notify();
					}
				}
				try {
					WebPage page = input.poll(50, TimeUnit.MILLISECONDS);
					if (page == null) 
						continue;
					page.categorize();
					output.offer(page);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		public void stop() {
			running = false;
		}
	}

}