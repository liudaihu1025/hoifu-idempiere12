package org.adempiere.model.engines;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author teo.sarca@gmail.com
 */
public class CostEngineFactory
{
	private static final ConcurrentHashMap<Integer, CostEngine> s_engines = new ConcurrentHashMap<>();
	
	public static CostEngine getCostEngine(int AD_Client_ID)
	{
		CostEngine engine = s_engines.get(AD_Client_ID);
		// Fallback to global engine
		if (engine == null && AD_Client_ID > 0)
		{
			engine = s_engines.get(0);
		}
		// Create Default Engine
		if (engine == null)
		{
			// computeIfAbsent 保证原子性，不会重复创建
			engine = s_engines.computeIfAbsent(AD_Client_ID, k -> new CostEngine());
			s_engines.put(AD_Client_ID, engine);
		}
		return engine;
	}
	
	public static void registerCostEngine(int AD_Client_ID, CostEngine engine)
	{
		s_engines.put(AD_Client_ID, engine);
	}
}
