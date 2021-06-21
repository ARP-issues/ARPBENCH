#include <mbgl/style/applied_class_properties.hpp>

namespace mbgl {

AppliedClassPropertyValue::AppliedClassPropertyValue(ClassID class_id, const TimePoint& begin_, const TimePoint& end_, const PropertyValue &value_)
    : name(class_id),
    begin(begin_),
    end(end_),
    value(value_) {}

// Returns the ID of the most recent
ClassID AppliedClassPropertyValues::mostRecent() const {
    return propertyValues.empty() ? ClassID::Fallback : propertyValues.back().name;
}

void AppliedClassPropertyValues::add(ClassID class_id, const TimePoint& begin, const TimePoint& end, const PropertyValue &value) {
    propertyValues.emplace_back(class_id, begin, end, value);
}

bool AppliedClassPropertyValues::hasTransitions() const {
    return propertyValues.size() > 1;
}

// Erase all items in the property list that are before a completed transition.
// Then, if the only remaining property is a Fallback value, remove it too.
void AppliedClassPropertyValues::cleanup(const TimePoint& now) {
    // Iterate backwards, but without using the rbegin/rend interface since we need forward
    // iterators to use .erase().
    for (auto it = propertyValues.end(), begin = propertyValues.begin(); it != begin;) {
        // If the property is finished, break iteration and delete all remaining items.
        if ((--it)->end <= now) {
            // Removes all items that precede the current iterator, but *not* the element currently
            // pointed to by the iterator. This preserves the last completed transition as the
            // first element in the property list.
            propertyValues.erase(begin, it);

            // Also erase the pivot element if it's a fallback value. This means we can remove the
            // entire applied properties object as well, because we already have the fallback
            // value set as the default.
            if (it->name == ClassID::Fallback) {
                propertyValues.erase(it);
            }
            break;
        }
    }
}

bool AppliedClassPropertyValues::empty() const {
    return propertyValues.empty();
}

}
