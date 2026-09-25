package com.freedomclient.module.utility;

import com.freedomclient.module.Category;
import com.freedomclient.module.Module;
import com.freedomclient.setting.BooleanSetting;

/** Updates: avisa en el menú principal cuando hay un FreedomClient nuevo y enseña las novedades tras actualizar. */
public class UpdatesModule extends Module {
	public final BooleanSetting whatsNew = add(new BooleanSetting("Show what's new", "Show the list of changes once after an update.", true));

	public UpdatesModule() {
		super("Updates", "Checks GitHub for a new FreedomClient version, installs it with one click and shows what's new.", Category.UTILITY, true);
	}

	@Override
	public boolean isVisibleInModuleList() {
		return false;
	}
}
