// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

/**
 * Proj Factory that creates instances from a given class.
 */
public class ClassProjFactory implements ProjFactory {

    private final Class<? extends Proj> projClass;

    public ClassProjFactory(Class<? extends Proj> projClass) {
        this.projClass = projClass;
    }

    @Override
    public Proj createInstance() {
        Proj proj = null;
        try {
            proj = projClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return proj;
    }
}
