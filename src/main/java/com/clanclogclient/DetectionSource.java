package com.clanclogclient;

enum DetectionSource
{
	COLLECTION_LOG_MESSAGE(6),
	COLLECTION_LOG_POPUP(5),
	VALUABLE_ITEM(4),
	UNTRADEABLE_ITEM(3),
	LOOT_TRACKER(2),
	OTHER(1);

	private final int priority;

	DetectionSource(int priority)
	{
		this.priority = priority;
	}

	boolean outranks(DetectionSource other)
	{
		return priority > other.priority;
	}
}
