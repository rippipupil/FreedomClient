package com.freedomclient.ui.menu;

import com.freedomclient.ui.Ui;

/** Contenido de una pestaña del menú, dibujado dentro del área que le da la ventana. */
public interface MenuPage {
	void render(Ui ui, int x, int y, int w, int h);
}
