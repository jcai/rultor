/**
 * Copyright (c) 2009-2014, rultor.com
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
package com.rultor.agents.req;

import com.jcabi.immutable.ArrayMap;
import com.jcabi.xml.XMLDocument;
import com.rultor.spi.Profile;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Tests for ${@link DockerRun}.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 1.0
 */
public final class DockerRunTest {

    /**
     * DockerRun can fetch environment vars.
     * @throws Exception In case of error.
     */
    @Test
    public void fetchesEnvVars() throws Exception {
        final Profile profile = new Profile.Fixed(
            new XMLDocument(
                StringUtils.join(
                    "<p><entry key='a'><entry key='env'>",
                    "<item>A=5</item><item>B=f e</item></entry></entry>",
                    "<entry key='b'><entry key='env'>HELLO='1'</entry></entry>",
                    "<entry key='c'><entry key='env'><entry key='MVN'>",
                    "works</entry></entry></entry></p>"
                )
            )
        );
        MatcherAssert.assertThat(
            new DockerRun(profile, "/p/entry[@key='a']").envs(
                new ArrayMap<String, String>()
            ),
            Matchers.equalTo("( '--env=A=5' '--env=B=f e' )")
        );
        MatcherAssert.assertThat(
            new DockerRun(profile, "/p/entry[@key='b']").envs(
                new ArrayMap<String, String>()
            ),
            Matchers.equalTo("( '--env=HELLO='\\''1'\\''' )")
        );
        MatcherAssert.assertThat(
            new DockerRun(profile, "/p/entry[@key='c']").envs(
                new ArrayMap<String, String>().with("X", "a\"'b")
            ),
            Matchers.equalTo("( '--env=MVN=works' '--env=X=a\"'\\''b' )")
        );
    }

    /**
     * DockerRun can fetch script.
     * @throws Exception In case of error.
     */
    @Test
    public void fetchesScript() throws Exception {
        final Profile profile = new Profile.Fixed(
            new XMLDocument(
                StringUtils.join(
                    "<p><entry key='x'><entry key='script'>",
                    "mvn clean</entry></entry>",
                    "<entry key='y'><entry key='script'>",
                    "<item>pw</item><item>ls</item></entry></entry></p>"
                )
            )
        );
        MatcherAssert.assertThat(
            new DockerRun(profile, "/p/entry[@key='x']").script(),
            Matchers.equalTo("( 'mvn clean' )")
        );
        MatcherAssert.assertThat(
            new DockerRun(profile, "/p/entry[@key='y']").script(),
            Matchers.equalTo("( 'pw' ';' 'ls' ';' )")
        );
    }

    /**
     * DockerRun can fetch from an empty doc.
     * @throws Exception In case of error.
     */
    @Test
    public void fetchesFromEmptyProfile() throws Exception {
        final Profile profile = new Profile.Fixed(new XMLDocument("<p/>"));
        final String empty = "(  )";
        MatcherAssert.assertThat(
            new DockerRun(profile, "/p/absent").envs(
                new ArrayMap<String, String>()
            ),
            Matchers.equalTo(empty)
        );
        MatcherAssert.assertThat(
            new DockerRun(profile, "/p/doesnt-exist").script(),
            Matchers.equalTo(empty)
        );
    }

    /**
     * DockerRun can fetch script.
     * @throws Exception In case of error.
     * @since 1.22
     */
    @Test
    public void fetchesInstallScript() throws Exception {
        final Profile profile = new Profile.Fixed(
            new XMLDocument(
                StringUtils.join(
                    "<p><entry key='f'><entry key='script'>hi</entry></entry>",
                    "<entry key='install'><item>one</item><item>two</item>",
                    "</entry></p>"
                )
            )
        );
        MatcherAssert.assertThat(
            new DockerRun(profile, "/p/entry[@key='f']").script(),
            Matchers.equalTo("( 'one' ';' 'two' ';' 'hi' )")
        );
    }

    /**
     * DockerRun can fetch environment vars.
     * @throws Exception In case of error.
     * @since 1.22
     */
    @Test
    public void fetchesEnvVarsDefaults() throws Exception {
        final Profile profile = new Profile.Fixed(
            new XMLDocument(
                StringUtils.join(
                    "<p><entry key='o'><entry key='env'>A=123</entry></entry>",
                    "<entry key='env'>ALPHA=909</entry></p>"
                )
            )
        );
        MatcherAssert.assertThat(
            new DockerRun(profile, "/p/entry[@key='o']").envs(
                new ArrayMap<String, String>()
            ),
            Matchers.equalTo("( '--env=ALPHA=909' '--env=A=123' )")
        );
    }

}