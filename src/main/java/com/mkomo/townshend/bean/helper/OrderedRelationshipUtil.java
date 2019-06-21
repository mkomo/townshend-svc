package com.mkomo.townshend.bean.helper;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OrderedRelationshipUtil {

	public static <T, R> Comparator<T> getOrderComparator(List<R> order, Function<T, R> mapper) {
		return new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				Integer i1 = order.indexOf(mapper.apply(o1));
				Integer i2 = order.indexOf(mapper.apply(o2));
				if (i1 < 0 && i2 < 0) {
					return 0;
				} else if (i1 >= 0 && i2 >= 0) {
					return i1.compareTo(i2);
				} else {
					return i2.compareTo(i1);
				}
			}
		};
	}

	//TODO change this away from ELF
	public static <T extends TownshendAuditable> Comparator<T> getOrderComparator(List<? extends Number> order) {
		final List<Long> idOrder = order != null
				? order.stream().map(val->(val == null ? null : val.longValue())).collect(Collectors.toList())
				: null;
		return new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				if (idOrder == null) {
					return compareDate(o1, o2);
				} else {
					Integer i1 = idOrder.indexOf(o1.getId());
					Integer i2 = idOrder.indexOf(o2.getId());
					if (i1 < 0 && i2 < 0) {
						return compareDate(o1, o2);
					} else if (i1 >= 0 && i2 >= 0) {
						return i1.compareTo(i2);
					} else {
						return i2.compareTo(i1);
					}
				}
			}
		};
	}


	private static <T extends TownshendAuditable> int compareDate(T o1, T o2) {
		if (o1.getDateCreated() == null) {
			return o2.getDateCreated() == null ? 0 : -1;
		} else {
			return o1.getDateCreated().compareTo(o2.getDateCreated());
		}
	}

	public static <T extends TownshendAuditable> EntityListField<? extends Number> determineOrder(List<T> items) {
		return items != null
				? new EntityListField<>(items.stream().map(q->q.getId()).collect(Collectors.toList()))
				: null;
	}

}
