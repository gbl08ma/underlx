package im.tny.segvault.s2ls.routing;

import im.tny.segvault.subway.Station;

/**
 * Created by Gabriel on 29/12/2017.
 */
public interface IAlternativeQualifier {
    boolean acceptable(Station station);
}
