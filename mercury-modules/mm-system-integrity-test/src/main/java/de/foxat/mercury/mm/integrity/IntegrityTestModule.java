package de.foxat.mercury.mm.integrity;

import de.foxat.mercury.api.MercuryModule;

public class IntegrityTestModule extends MercuryModule {

    @Override
    protected void onLoad() {
        System.out.println("onLoad from Module");
    }

    @Override
    protected void onEnable() {
        System.out.println("onEnable from Module");
    }

    @Override
    protected void onDisable() {
        System.out.println("onDisable from Module");
    }
}
