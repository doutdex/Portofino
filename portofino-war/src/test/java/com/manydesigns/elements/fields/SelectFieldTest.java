/*
 * Copyright (C) 2005-2010 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * There are special exceptions to the terms and conditions of the GPL
 * as it is applied to this software. View the full text of the
 * exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
 * software distribution.
 *
 * This program is distributed WITHOUT ANY WARRANTY; and without the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
 * or write to:
 * Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307  USA
 *
 */

package com.manydesigns.elements.fields;

import com.manydesigns.elements.AbstractElementsTest;
import com.manydesigns.elements.Mode;
import com.manydesigns.elements.options.DefaultSelectionProvider;
import com.manydesigns.elements.options.SelectionModel;
import com.manydesigns.elements.options.SelectionProvider;
import com.manydesigns.elements.reflection.ClassAccessor;
import com.manydesigns.elements.reflection.JavaClassAccessor;
import com.manydesigns.elements.reflection.PropertyAccessor;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
*/
public class SelectFieldTest extends AbstractElementsTest {
    public static final String copyright =
            "Copyright (c) 2005-2010, ManyDesigns srl";

    public String myText;

    private SelectField selectField;
    private SelectField selectField2;

    private String[][] valuesArray = {
            {"value1"},
            {"value2"},
            {"value3"}
    };
    private String[][] labelsArray = {
            {"label1"},
            {"label2"},
            {"label3"}
    };

    private String[][] valuesArray2 = {
            {"value1"}
    };
    private String[][] labelsArray2 = {
            {"label1"}
    };

    private SelectionProvider selectionProvider;
    private SelectionModel selectionModel;

    private SelectionProvider selectionProvider2;
    private SelectionModel selectionModel2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        myText = null;

        selectionProvider = DefaultSelectionProvider.create(
                "selectionProvider", 1, valuesArray, labelsArray);
        selectionModel = selectionProvider.createSelectionModel();

        selectionProvider2 = DefaultSelectionProvider.create(
                "selectionProvider", 1, valuesArray2, labelsArray2);
        selectionModel2 = selectionProvider2.createSelectionModel();
    }

    private void setupSelectFields(Mode mode) {
        ClassAccessor classAccessor =
                JavaClassAccessor.getClassAccessor(this.getClass());
        PropertyAccessor myPropertyAccessor =
                null;
        try {
            myPropertyAccessor = classAccessor.getProperty("myText");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            fail();
        }

        // impostiamo selectField1
        selectField = new SelectField(myPropertyAccessor, mode, null);
        selectField.setSelectionModel(selectionModel);
        // impostiamo selectField2
        selectField2 = new SelectField(myPropertyAccessor, mode, null);
        selectField2.setSelectionModel(selectionModel2);
    }

    public void testSimple() {
        setupSelectFields(Mode.EDIT);

        assertNotNull(selectField.getComboLabel());
        assertEquals("-- Select my text --", selectField.getComboLabel());
        assertNull(selectField.getValue());
        assertNotNull(selectField.getErrors());
        assertEquals(0, selectField.getErrors().size());
        assertNull(selectField.getHelp());
        assertEquals("myText", selectField.getId());
        assertEquals("myText", selectField.getInputName());
        assertEquals("my text", selectField.getLabel());
        assertEquals(Mode.EDIT, selectField.getMode());
        assertFalse(selectField.isRequired());
        assertEquals(3, selectField.getOptions().size());
        assertEquals(1, selectField2.getOptions().size());
    }

    //--------------------------------------------------------------------------
    // Mode.EDIT
    //--------------------------------------------------------------------------

    public void testEditNull() {
        setupSelectFields(Mode.EDIT);
        String text = elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"field\">My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\" selected=\"selected\">-- Select my text --</option>" +
                "<option value=\"value1\">label1</option>" +
                "<option value=\"value2\">label2</option>" +
                "<option value=\"value3\">label3</option>" +
                "</select></td>", text);
    }

    public void testEditNullRequired() {
        setupSelectFields(Mode.EDIT);

        selectField.setRequired(true);
        String text = elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"field\">" +
                "<span class=\"required\">*</span>&nbsp;My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\" selected=\"selected\">-- Select my text --</option>" +
                "<option value=\"value1\">label1</option>" +
                "<option value=\"value2\">label2</option>" +
                "<option value=\"value3\">label3</option>" +
                "</select></td>", text);

        assertFalse(selectField.validate());
        text = elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"field\">" +
                "<span class=\"required\">*</span>&nbsp;My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\" selected=\"selected\">-- Select my text --</option>" +
                "<option value=\"value1\">label1</option>" +
                "<option value=\"value2\">label2</option>" +
                "<option value=\"value3\">label3</option>" +
                "</select><ul class=\"errors\">" +
                "<li>Required field" +
                "</li></ul></td>", text);
    }

    public void testEditNullWithComboLabel() {
        setupSelectFields(Mode.EDIT);

        selectField.setComboLabel("-- Scegli opzione --");
        String text = elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"field\">My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\" selected=\"selected\">-- Scegli opzione --</option>" +
                "<option value=\"value1\">label1</option>" +
                "<option value=\"value2\">label2</option>" +
                "<option value=\"value3\">label3</option>" +
                "</select></td>", text);
        assertEquals("-- Scegli opzione --", selectField.getComboLabel());
    }

    public void testEditValidSelection() {
        setupSelectFields(Mode.EDIT);

        selectField.setValue("value1");
        String text = elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"field\">My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\">-- Select my text --</option>" +
                "<option value=\"value1\" selected=\"selected\">label1</option>" +
                "<option value=\"value2\">label2</option>" +
                "<option value=\"value3\">label3</option>" +
                "</select></td>", text);
    }

    public void testEditInvalidSelection() {
        setupSelectFields(Mode.EDIT);

        selectField.setValue("value4");
        String text = elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"field\">My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\" selected=\"selected\">-- Select my text --</option>" +
                "<option value=\"value1\">label1</option>" +
                "<option value=\"value2\">label2</option>" +
                "<option value=\"value3\">label3</option>" +
                "</select></td>", text);
    }

    //--------------------------------------------------------------------------
    // Mode.VIEW
    //--------------------------------------------------------------------------

    public void testViewNull() {
        setupSelectFields(Mode.VIEW);

        String text = elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"field\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\"></div>" +
                "</td>", text);
    }

    public void testViewValidSelection() {
        setupSelectFields(Mode.VIEW);

        selectField.setValue("value1");
        String text = elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"field\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\">label1</div>" +
                "</td>", text);
    }

    public void testViewValidSelectionNoUrl() {
        setupSelectFields(Mode.VIEW);

        selectField.setValue("value3");
        String text = elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"field\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\">label3</div>" +
                "</td>", text);
    }

    public void testViewInvalidSelection() {
        setupSelectFields(Mode.VIEW);

        selectField.setValue("value4");
        String text = elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"field\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\"></div>" +
                "</td>", text);
    }

    //--------------------------------------------------------------------------
    // Mode.PREVIEW
    //--------------------------------------------------------------------------

    public void testPreviewNull() {
        setupSelectFields(Mode.PREVIEW);

        String text = elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"field\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\"></div>" +
                "<input type=\"hidden\" id=\"myText\" name=\"myText\"></input>" +
                "</td>", text);
    }

    public void testPreviewValidSelection() {
        setupSelectFields(Mode.PREVIEW);

        selectField.setValue("value1");
        String text = elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"field\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\">label1</div>" +
                "<input type=\"hidden\" id=\"myText\" name=\"myText\" value=\"value1\"></input>" +
                "</td>", text);
    }

    public void testPreviewValidSelectionNoUrl() {
        setupSelectFields(Mode.PREVIEW);

        selectField.setValue("value3");
        String text = elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"field\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\">label3</div>" +
                "<input type=\"hidden\" id=\"myText\" name=\"myText\" value=\"value3\"></input>" +
                "</td>", text);
    }

    public void testPreviewInvalidSelection() {
        setupSelectFields(Mode.PREVIEW);

        selectField.setValue("value4");
        String text = elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"field\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\"></div>" +
                "<input type=\"hidden\" id=\"myText\" name=\"myText\"></input>" +
                "</td>", text);
    }


    //--------------------------------------------------------------------------
    // Mode.HIDDEN
    //--------------------------------------------------------------------------

    public void testHiddenNull() {
        setupSelectFields(Mode.HIDDEN);

        String text = elementToString(selectField);
        assertEquals("<input type=\"hidden\" id=\"myText\" name=\"myText\"></input>", text);
    }

    public void testHiddenValidSelection() {
        setupSelectFields(Mode.HIDDEN);

        selectField.setValue("value1");
        String text = elementToString(selectField);
        assertEquals("<input type=\"hidden\" id=\"myText\" name=\"myText\" value=\"value1\"></input>", text);
    }

    public void testHiddenValidSelectionNoUrl() {
        setupSelectFields(Mode.HIDDEN);

        selectField.setValue("value3");
        String text = elementToString(selectField);
        assertEquals("<input type=\"hidden\" id=\"myText\" name=\"myText\" value=\"value3\"></input>", text);
    }

    public void testHiddenInvalidSelection() {
        setupSelectFields(Mode.HIDDEN);

        selectField.setValue("value4");
        String text = elementToString(selectField);
        assertEquals("<input type=\"hidden\" id=\"myText\" name=\"myText\"></input>", text);
    }

    //--------------------------------------------------------------------------
    // metodi di Element
    //--------------------------------------------------------------------------

    public void testReadFromRequestWrongValue() {
        setupSelectFields(Mode.EDIT);

        assertFalse(selectField.isRequired());

        req.setParameter("myText", "wrongValue");
        assertNull(selectField.getValue());
        selectField.readFromRequest(req);
        assertNull(selectField.getValue());
    }

    public void testReadFromRequest() {
        setupSelectFields(Mode.EDIT);

        assertFalse(selectField.isRequired());

        req.setParameter("myText", "value1");
        assertNull(selectField.getValue());
        selectField.readFromRequest(req);
        assertEquals("value1", selectField.getValue());
    }

    public void testReadFromRequestRequired() {
        setupSelectFields(Mode.EDIT);

        assertEquals(1, selectField2.getOptions().size());
        selectField2.setRequired(true);

        assertNull(selectField2.getValue());
        selectField2.readFromRequest(req);
        String text = elementToString(selectField2);
        assertEquals("<th><label for=\"myText\" class=\"field\">" +
                "<span class=\"required\">*</span>&nbsp;My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\" selected=\"selected\">-- Select my text --</option>" +
                "<option value=\"value1\">label1</option>" +
                "</select></td>", text);
    }

    public void testReadFromRequestNotRequired() {
        setupSelectFields(Mode.EDIT);

        assertFalse(selectField2.isRequired());
        assertEquals(1, selectField2.getOptions().size());

        assertNull(selectField2.getValue());
        selectField2.readFromRequest(req);
        assertNull(selectField2.getValue());
    }

    public void testReadFromObject() {
        setupSelectFields(Mode.EDIT);

        assertNull(selectField.getValue());
        myText = "value2";
        selectField.readFromObject(this);
        assertEquals("value2", selectField.getValue());
    }

    public void testWriteToObject() {
        setupSelectFields(Mode.EDIT);

        assertNull(myText);
        selectField.setValue("value2");
        selectField.writeToObject(this);
        assertEquals("value2", myText);
    }
}