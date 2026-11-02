package ua.kvitka.ai;

import java.util.*;
import java.util.concurrent.*;

/** Concurrent RSS download with one overall deadline and independent source failures. */
public final class FeedLoader {
    @FunctionalInterface public interface Fetcher { List<NewsArticle> fetch(FeedSource source) throws Exception; }
    public record Result(List<NewsArticle> articles, int successful, List<String> failures) {
        public Result { articles=List.copyOf(articles); failures=List.copyOf(failures); }
    }
    private final Fetcher fetcher;
    private final long deadlineMs;
    public FeedLoader() { this(new RssReader()::fetch, 30_000); }
    public FeedLoader(Fetcher fetcher, long deadlineMs) {
        if (deadlineMs<1) throw new IllegalArgumentException("deadline must be positive");
        this.fetcher=Objects.requireNonNull(fetcher);this.deadlineMs=deadlineMs;
    }
    private record Completed(FeedSource source, List<NewsArticle> articles, Exception problem) {}
    public Result load(List<FeedSource> sources) throws InterruptedException {
        if (sources.isEmpty()) return new Result(List.of(), 0, List.of());
        int count=sources.size();
        ExecutorService pool=Executors.newFixedThreadPool(Math.min(4,count));
        ExecutorCompletionService<Completed> completion=new ExecutorCompletionService<>(pool);
        Map<Future<Completed>,FeedSource> pending=new LinkedHashMap<>();
        List<NewsArticle> articles=new ArrayList<>();
        List<String> failures=new ArrayList<>();
        int successful=0;
        long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(deadlineMs);
        try {
            for(FeedSource source:sources) {
                Future<Completed> f=completion.submit(()->{
                    try { return new Completed(source,fetcher.fetch(source),null); }
                    catch(Exception e) { return new Completed(source,List.of(),e); }
                });
                pending.put(f,source);
            }
            while(!pending.isEmpty()) {
                long remaining=deadline-System.nanoTime();
                if(remaining<=0) break;
                Future<Completed> f=completion.poll(remaining,TimeUnit.NANOSECONDS);
                if(f==null) break;
                FeedSource source=pending.remove(f);
                if(source==null)continue;
                try {
                    Completed completed=f.get();
                    if(completed.problem()!=null)failures.add(source.name()+" ("+reason(completed.problem())+")");
                    else if(completed.articles()==null||completed.articles().isEmpty())failures.add(source.name()+" (порожня RSS-стрічка)");
                    else {articles.addAll(completed.articles());successful++;}
                } catch(ExecutionException | CancellationException e) {
                    failures.add(source.name()+" ("+reason(e)+")");
                }
            }
            for(var entry:pending.entrySet()) {
                entry.getKey().cancel(true);
                failures.add(entry.getValue().name()+" (тайм-аут)");
            }
            return new Result(articles,successful,failures);
        } finally {pool.shutdownNow();}
    }
    private static String reason(Throwable t) {
        Throwable root=t;
        while(root instanceof ExecutionException && root.getCause()!=null)root=root.getCause();
        if(root instanceof java.net.SocketTimeoutException || root instanceof TimeoutException)return "тайм-аут";
        if(root instanceof java.net.UnknownHostException)return "немає доступу до сервера";
        if(root instanceof java.net.ConnectException)return "не вдалося підключитися";
        String message=root.getMessage();
        if(message==null||message.isBlank())message=root.getClass().getSimpleName();
        message=message.replaceAll("[\\r\\n\\t]+"," ");
        return message.length()>90?message.substring(0,87)+"...":message;
    }
}
