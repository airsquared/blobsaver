/*
 * Copyright (c) 2021  airsquared
 *
 * This file is part of blobsaver.
 *
 * blobsaver is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * blobsaver is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with blobsaver.  If not, see <https://www.gnu.org/licenses/>.
 */

package airsquared.blobsaver.app;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FXMLTest {

    @Test
    public void loadFXML() throws IOException {
        // run in headless mode
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");

        Platform.startup(() -> {
        });

        HBox box = FXMLLoader.load(Main.class.getResource("blobsaver.fxml"));
        List<Node> children = box.getChildren();
        assertTrue(children.get(0) instanceof VBox);
        assertTrue(children.get(1) instanceof VBox);
        assertEquals(children.get(1).getId(), "savedDevicesVBox");

        Platform.exit();
    }

}
