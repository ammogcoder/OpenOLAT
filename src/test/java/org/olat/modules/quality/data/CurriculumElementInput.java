/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.quality.data;

import org.olat.modules.curriculum.Curriculum;
import org.olat.modules.curriculum.CurriculumElement;

/**
 * 
 * Initial date: 05.09.2018<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class CurriculumElementInput {

	private final String displayName;
	private final String identifier;
	private final Curriculum curriculum;
	private final CurriculumElement parent;
	
	public CurriculumElementInput(String displayName, String identifier, Curriculum curriculum,
			CurriculumElement parent) {
		this.displayName = displayName;
		this.identifier = identifier;
		this.curriculum = curriculum;
		this.parent = parent;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getIdentifier() {
		return identifier;
	}

	public Curriculum getCurriculum() {
		return curriculum;
	}

	public CurriculumElement getParent() {
		return parent;
	}
	
}
