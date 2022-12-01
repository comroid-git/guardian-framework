package org.comroid.common.os;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Named;
import org.comroid.api.UUIDContainer;
import org.comroid.api.Disposable;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.mutatio.pipe.EventPipeline;
import org.comroid.mutatio.ref.ReferencePipe;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.io.*;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

@ApiStatus.Experimental
public final class FileSocket extends UUIDContainer.Base implements EventPipeline<String, String>, Disposable, Named {
    private final File file;
    private final Logger logger;
    private final ReferencePipe<String, String, String, String> pipe;
    private final Scanner fsock;

    @Override
    public RefContainer<String, String> getEventPipeline() {
        return pipe;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Internal
    public FileSocket(File file, Executor executor) throws IOException, FileNotFoundException {
        this.file = Objects.requireNonNull(file, "File is null");
        this.logger = LogManager.getLogger("FileSocket:" + getName());
        this.pipe = new ReferencePipe<>(executor);

        if (file.exists() && !file.delete())
            throw new IOException("Could not delete old File: " + file);
        if (!(file.getParentFile().mkdirs() || file.createNewFile()))
            throw new IOException("Could not create File: " + file);
        FileInputStream fis = new FileInputStream(file);
        this.fsock = new Scanner(fis);

        addChildren(fis, fsock); // todo: maybe reverse order?

        executor.execute(() -> {
            logger.trace("Receiver started for file: {}", this.file);
            try {
                while (true) {
                    try {
                        pipe.accept("data", fsock.nextLine());
                    } catch (NoSuchElementException e) {
                        if (!e.getMessage().equals("No line found"))
                            throw e;
                    }
                }
            } catch (Throwable t) {
                logger.fatal("Encountered a fatal problem; closing down", t);
                disposeThrow();
            }
        });
    }

    public static FileSocket openReceiver(String name) throws IOException {
        return openReceiver(name, ContextualProvider.getFromRoot(Executor.class, ForkJoinPool::commonPool));
    }

    @Internal
    public static File getFile(String forName) throws IOException {
        return File.createTempFile("org.comroid.fsock/" + forName, ".fsock");
    }

    public static FileSocket openReceiver(String name, Executor executor) throws IOException {
        return new FileSocket(getFile(name), executor);
    }

    public static FileSocket.Sender openSender(String name) throws FileNotFoundException, IOException {
        return new FileSocket.Sender(getFile(name));
    }

    public static final class Sender extends UUIDContainer.Base implements Consumer<String>, Named, Disposable {
        private final File file;
        private final Logger logger;
        private final PrintWriter fsock;

        @Override
        public String getName() {
            return file.getName();
        }

        public Sender(File file) throws FileNotFoundException {
            this.file = file;
            this.logger = LogManager.getLogger("FileSocket.Sender:" + getName());
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            this.fsock = new PrintWriter(osw);

            addChildren(fos, osw, fsock); // todo: maybe reverse order?

            logger.trace("Sender started for file {}", file);
        }

        @Override
        public void accept(String eventData) {
            logger.trace("Publishing event data: {}", eventData);
            fsock.println(eventData);
        }
    }
}
