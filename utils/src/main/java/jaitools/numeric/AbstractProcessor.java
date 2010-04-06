/*
 * Copyright 2009-2010 Michael Bedward
 *
 * This file is part of jai-tools.
 *
 * jai-tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * jai-tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with jai-tools.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package jaitools.numeric;

import jaitools.CollectionFactory;
import jaitools.numeric.Range.Type;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for statistics processors used with {@code StreamingSampleStats}.
 *
 * @author Michael Bedward
 * @since 1.0
 * @source $URL$
 * @version $Id$
 */
public abstract class AbstractProcessor implements Processor {

    protected long numOffered;
    protected long numAccepted;
    protected long numNaN;

    private List<Range<Double>> ranges;
    private Range.Type rangesType;

    /**
     * Default constructor.
     */
    public AbstractProcessor() {
        ranges = CollectionFactory.list();
        rangesType = Range.Type.UNDEFINED;
        numOffered = numAccepted = 0;
    }
    
    /**
     * Default constructor.
     */
    public AbstractProcessor(final Range.Type rangesType) {
        ranges = CollectionFactory.list();
        this.rangesType = rangesType;
        numOffered = numAccepted = 0;
    }

    /**
     * {@inheritDoc}
     *
     * Adding a {@code Range} that overlaps with one or more existing
     * {@code Ranges} is permitted.
     */
    public void addExcludedRange(Range<Double> exclude) {
        if (exclude != null) {
            // copy the input Range defensively
            ranges.add(new Range<Double>(exclude));
        }
    }

    /**
     * {@inheritDoc}
     * @deprecated use {@link #getRanges()}
     */
    public List<Range<Double>> getExcludedRanges() {
        return Collections.unmodifiableList(ranges);
    }

    /**
     * {@inheritDoc}.
     * Null and Double.NaN values are excluded by default.
     * @deprecated use {@link #isAccepted(Double)} with opposite logic.
     */
    public boolean isExcluded(Double sample) {
        if (sample == null){
            return true;
        }
        if (sample.isNaN()) {
            numNaN++;
            return true;
        }

        for (Range<Double> r : ranges) {
        	switch (rangesType){
        		case EXCLUDED:
        			return r.contains(sample);
        		case INCLUDED:
        			return !r.contains(sample);
        	}
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public final long getNumOffered() {
        return numOffered;
    }

    /**
     * {@inheritDoc}
     */
    public final long getNumAccepted() {
        return numAccepted;
    }
    
    /**
     * {@inheritDoc}
     */
    public long getNumNaN() {
        return numNaN;
    }

    /**
     * {@inheritDoc}
     */
    public void offer(Double sample) {
        numOffered++;
        if (update(sample)) {
            numAccepted++ ;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<Statistic, Double> get() {
        Map<Statistic, Double> stats = new HashMap<Statistic, Double>();
        for (Statistic s : getSupported()) {
            stats.put(s, get(s));
        }
        return stats;
    }

    /**
     * Process a sample value that has been offered by the client.
     *
     * @param sample the sample value
     * 
     * @return true if the sample is accepted (ie. used for calculations);
     *         false otherwise
     */
    protected abstract boolean update(Double sample);

    /**
     * {@inheritDoc}
     */
	public void addRange(Range<Double> range) {
		if (range != null) {
			if (this.rangesType == Range.Type.UNDEFINED){
				this.rangesType = Range.Type.EXCLUDED;
			}
			// copy the input Range defensively
            ranges.add(new Range<Double>(range));
        }
	}
	
	/**
     * {@inheritDoc}
     */
	public void addRange(Range<Double> range, Range.Type rangesType) {
		if (range != null) {
			if (this.rangesType == Range.Type.UNDEFINED){
				this.rangesType = rangesType;
			} else {
				if (this.rangesType != rangesType){
					throw new IllegalArgumentException("The provided rangesType is not compatible with the processors rangesType");
				}
			}
			// copy the input Range defensively
            ranges.add(new Range<Double>(range));
        }
	}

	 /**
     * {@inheritDoc}
     */
	public List<Range<Double>> getRanges() {
		return Collections.unmodifiableList(ranges);
	}

	 /**
     * {@inheritDoc}
     */
	public void setRangesType(final Range.Type rangesType) {
		if (this.rangesType != Range.Type.UNDEFINED){
			throw new UnsupportedOperationException("Cannot change RangesType once already defined");
		}
		this.rangesType = rangesType; 
	}
	
	 /**
     * {@inheritDoc}
     */
	public final Range.Type getRangesType(){
		return rangesType;
	}

	 /**
     * {@inheritDoc}
     */
	public boolean isAccepted(Double sample) {
		if (sample == null){
            return false;
        }
        if (sample.isNaN()) {
            numNaN++;
            return false;
        }

    	for (Range<Double> r : ranges) {
        	switch (rangesType){
        		case EXCLUDED:
        			return !r.contains(sample);
        		case INCLUDED:
        			return r.contains(sample);
        	}
        }

        return true;
	}

}
