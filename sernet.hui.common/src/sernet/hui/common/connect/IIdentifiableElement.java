/*******************************************************************************
 * Copyright (c) 2018 Jochen Kemnade.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/
package sernet.hui.common.connect;

/**
 * An interface for elements that have an identifier
 */
public interface IIdentifiableElement extends ITitledElement {

    String getIdentifier();

    /**
     * Returns a combination of the element's identifier and its title.
     * If either is null or empty, it returns the other one, if both are empty, it returns the empty string.
     */
    String getFullTitle();

}
