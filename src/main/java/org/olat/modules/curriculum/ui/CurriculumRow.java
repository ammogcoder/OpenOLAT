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
package org.olat.modules.curriculum.ui;

import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.modules.curriculum.Curriculum;
import org.olat.modules.curriculum.CurriculumRef;
import org.olat.modules.curriculum.model.CurriculumInfos;

/**
 * 
 * Initial date: 13 févr. 2018<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CurriculumRow implements CurriculumRef {
	
	private final Curriculum curriculum;
	private final long numOfElements;
	
	private final FormLink toolsLink;
	
	public CurriculumRow(Curriculum curriculum) {
		this.curriculum = curriculum;
		numOfElements = -1l;
		toolsLink = null;
	}
	
	public CurriculumRow(CurriculumInfos infos, FormLink toolsLink) {
		curriculum = infos.getCurriculum();
		numOfElements = infos.getNumOfElements();
		this.toolsLink = toolsLink;
	}
	
	@Override
	public Long getKey() {
		return curriculum.getKey();
	}
	
	public String getDisplayName() {
		return curriculum.getDisplayName();
	}
	
	public String getIdentifier() {
		return curriculum.getIdentifier();
	}
	
	public String getExternalId() {
		return curriculum.getExternalId();
	}
	
	public long getNumOfElements() {
		return numOfElements;
	}
	
	public FormLink getTools() {
		return toolsLink;
	}
}
