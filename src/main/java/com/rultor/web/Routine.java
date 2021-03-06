/**
 * Copyright (c) 2009-2015, rultor.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the rultor.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rultor.web;

import co.stateful.Sttc;
import com.jcabi.aspects.ScheduleWithFixedDelay;
import com.jcabi.aspects.Timeable;
import com.jcabi.aspects.Tv;
import com.jcabi.github.Github;
import com.jcabi.log.Logger;
import com.rultor.Toggles;
import com.rultor.agents.Agents;
import com.rultor.profiles.Profiles;
import com.rultor.spi.Profile;
import com.rultor.spi.Pulse;
import com.rultor.spi.Talk;
import com.rultor.spi.Talks;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.validation.constraints.NotNull;

/**
 * Routine.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 1.50
 */
@ScheduleWithFixedDelay(delay = 1, unit = TimeUnit.MINUTES, threads = 1)
@SuppressWarnings("PMD.DoNotUseThreads")
final class Routine implements Runnable, Closeable {

    /**
     * Shutting down?
     */
    private final transient AtomicBoolean down = new AtomicBoolean();

    /**
     * Ticks.
     */
    private final transient Collection<Pulse.Tick> list;

    /**
     * Talks.
     */
    private final transient Talks talks;

    /**
     * Agents.
     */
    private final transient Agents agents;

    /**
     * Ctor.
     * @param tlks Talks
     * @param ticks Ticks
     * @param github Github client
     * @param sttc Sttc client
     * @checkstyle ParameterNumberCheck (4 lines)
     */
    Routine(@NotNull final Talks tlks, final Collection<Pulse.Tick> ticks,
        final Github github, final Sttc sttc) {
        this.talks = tlks;
        this.list = ticks;
        this.agents = new Agents(github, sttc);
    }

    @Override
    public void close() {
        this.down.set(true);
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void run() {
        try {
            this.safe();
            // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Exception ex) {
            if (!this.down.get()) {
                try {
                    TimeUnit.MICROSECONDS.sleep(1L);
                } catch (final InterruptedException iex) {
                    Logger.info(this, "%[exception]s", iex);
                }
            }
        }
    }

    /**
     * Routine every-minute proc.
     * @return Total talks processed
     * @throws IOException If fails
     */
    private int process() throws IOException {
        this.agents.starter().execute(this.talks);
        final Profiles profiles = new Profiles();
        int total = 0;
        for (final Talk talk : this.talks.active()) {
            ++total;
            final Profile profile = profiles.fetch(talk);
            this.agents.agent(talk, profile).execute(talk);
        }
        this.agents.closer().execute(this.talks);
        return total;
    }

    /**
     * Routine every-minute proc.
     * @return Milliseconds spent
     * @throws IOException If fails
     */
    @Timeable(limit = Tv.FIVE, unit = TimeUnit.MINUTES)
    private long safe() throws IOException {
        final long start = System.currentTimeMillis();
        int total = 0;
        if (new Toggles().readOnly()) {
            Logger.info(this, "read-only mode");
        } else {
            total = this.process();
        }
        final long msec = System.currentTimeMillis() - start;
        if (!this.list.add(new Pulse.Tick(start, msec, total))) {
            throw new IllegalStateException("failed to add tick");
        }
        return msec;
    }

}
