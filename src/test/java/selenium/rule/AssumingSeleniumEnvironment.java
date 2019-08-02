/*
 * Copyright (c) 2019. The Ontario Institute for Cancer Research. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package selenium.rule;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import selenium.driver.WebDriverFactory.DriverType;

public class AssumingSeleniumEnvironment implements TestRule {

  private SeleniumEnvironmentChecker checker;

  public AssumingSeleniumEnvironment(SeleniumEnvironmentChecker checker) {
    this.checker = checker;
  }

  public DriverType getDriverType() {
    return checker.getType();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        if (!checker.shouldRunTest()) {
          throw new AssumptionViolatedException("Could not connect. Skipping test!");
        } else {
          base.evaluate();
        }
      }
    };
  }
}
