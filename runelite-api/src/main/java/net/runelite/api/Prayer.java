/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.api;

import net.runelite.api.annotations.Varbit;
import net.runelite.api.widgets.WidgetInfo;

/**
 * An enumeration of different prayer spells.
 */
public enum Prayer
{
/**
	 * Protect from Magic (Level 37).
	 */
	PROTECT_FROM_MAGIC(Varbits.PRAYER_PROTECT_FROM_MAGIC, 20.0, WidgetInfo.PRAYER_PROTECT_FROM_MAGIC),
	/**
	 * Protect from Missiles (Level 40).
	 */
	PROTECT_FROM_MISSILES(Varbits.PRAYER_PROTECT_FROM_MISSILES, 20.0, WidgetInfo.PRAYER_PROTECT_FROM_MISSILES),
	/**
	 * Protect from Melee (Level 43).
	 */
	PROTECT_FROM_MELEE(Varbits.PRAYER_PROTECT_FROM_MELEE, 20.0, WidgetInfo.PRAYER_PROTECT_FROM_MELEE),
	/**
	 * Eagle Eye (Level 44, Ranging).
	 */
	EAGLE_EYE(Varbits.PRAYER_EAGLE_EYE, 20.0, WidgetInfo.PRAYER_EAGLE_EYE),
	/**
	 * Mystic Might (Level 45, Magic).
	 */
	MYSTIC_MIGHT(Varbits.PRAYER_MYSTIC_MIGHT, 20.0, WidgetInfo.PRAYER_MYSTIC_MIGHT),
	/**
	 * Piety (Level 70, Defence/Strength/Attack).
	 */
	PIETY(Varbits.PRAYER_PIETY, 40.0, WidgetInfo.PRAYER_PIETY),
	/**
	 * Rigour (Level 74, Ranging/Damage/Defence).
	 */
	RIGOUR(Varbits.PRAYER_RIGOUR, 40.0, WidgetInfo.PRAYER_RIGOUR),
	/**
	 * Augury (Level 77, Magic/Magic Def./Defence).
	 */
	AUGURY(Varbits.PRAYER_AUGURY, 40.0, WidgetInfo.PRAYER_AUGURY);

	private final int varbit;
	private final double drainRate;
	private final WidgetInfo widgetInfo;

	Prayer(@Varbit int varbit, double drainRate, WidgetInfo widgetInfo)
	{
		this.varbit = varbit;
		this.drainRate = drainRate;
		this.widgetInfo = widgetInfo;
	}

	/**
	 * Gets the varbit that stores whether the prayer is active or not.
	 *
	 * @return the prayer active varbit
	 */
	@Varbit
	public int getVarbit()
	{
		return varbit;
	}

	/**
	 * Gets the prayer drain rate (measured in pray points/minute)
	 *
	 * @return the prayer drain rate
	 */
	public double getDrainRate()
	{
		return drainRate;
	}

	public WidgetInfo getWidgetInfo()
	{
		return widgetInfo;
	}
}
