package by.bsuir.insurance.server.it;

import by.bsuir.insurance.common.model.Application;
import by.bsuir.insurance.common.model.InsuranceType;
import by.bsuir.insurance.common.protocol.CommandType;
import by.bsuir.insurance.common.protocol.Request;
import by.bsuir.insurance.common.protocol.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Нагрузочная проверка многопоточного сервера: N клиентов одновременно подключаются,
 * входят в систему и выполняют по 10 запросов (список видов страхования и расчёт премии).
 * Запуск при работающем сервере: java ... LoadTest 10 50 100
 */
public final class LoadTest {
    private static final int REQUESTS = 10;

    private LoadTest() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Клиентов\tЗапросов\tОшибок\tОбщее время, мс\tСреднее время запроса, мс\tМакс. время запроса, мс");
        for (String a : args) {
            run(Integer.parseInt(a));
        }
    }

    private static void run(int clients) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(clients);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<long[]>> futures = new ArrayList<>();
        for (int i = 0; i < clients; i++) {
            futures.add(pool.submit(task(start)));
        }
        long t0 = System.nanoTime();
        start.countDown();
        List<Long> times = new ArrayList<>();
        int errors = 0;
        for (Future<long[]> f : futures) {
            try {
                for (long t : f.get()) {
                    if (t < 0) {
                        errors++;
                    } else {
                        times.add(t);
                    }
                }
            } catch (Exception e) {
                errors += REQUESTS + 1;
            }
        }
        long total = (System.nanoTime() - t0) / 1_000_000;
        pool.shutdown();
        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
        double max = (times.isEmpty() ? 0 : Collections.max(times)) / 1_000_000.0;
        System.out.printf("%d\t%d\t%d\t%d\t%.1f\t%.1f%n", clients, clients * (REQUESTS + 1), errors, total, avg, max);
    }

    private static Callable<long[]> task(CountDownLatch start) {
        return () -> {
            start.await();
            long[] t = new long[REQUESTS + 1];
            try (Socket s = new Socket("localhost", 5555)) {
                s.setTcpNoDelay(true);
                ObjectOutputStream out = new ObjectOutputStream(new java.io.BufferedOutputStream(s.getOutputStream()));
                out.flush();
                ObjectInputStream in = new ObjectInputStream(new java.io.BufferedInputStream(s.getInputStream()));
                t[0] = timed(out, in, new Request(CommandType.LOGIN).with("login", "ivanov").with("password", "client123"));
                List<InsuranceType> types = null;
                for (int i = 1; i <= REQUESTS; i++) {
                    if (i % 2 == 1 || types == null) {
                        long t1 = System.nanoTime();
                        Response r = send(out, in, new Request(CommandType.GET_ACTIVE_TYPES));
                        t[i] = r.isSuccess() ? System.nanoTime() - t1 : -1;
                        types = r.getData();
                    } else {
                        Application a = new Application();
                        a.setTypeId(types.get(0).getId());
                        a.setRequestedSum(new BigDecimal("50000"));
                        a.setTermMonths(12);
                        a.setFranchisePercent(0);
                        a.setInstallments(1);
                        a.setObjectDescription("Квартира");
                        t[i] = timed(out, in, new Request(CommandType.CALCULATE_PREMIUM).with("application", a));
                    }
                }
            }
            return t;
        };
    }

    private static long timed(ObjectOutputStream out, ObjectInputStream in, Request r) throws Exception {
        long t1 = System.nanoTime();
        return send(out, in, r).isSuccess() ? System.nanoTime() - t1 : -1;
    }

    private static Response send(ObjectOutputStream out, ObjectInputStream in, Request r) throws Exception {
        out.writeObject(r);
        out.reset();
        out.flush();
        return (Response) in.readObject();
    }
}
