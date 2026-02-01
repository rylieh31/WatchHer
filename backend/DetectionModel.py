from sklearn.ensemble import RandomForestClassifier
from sklearn.tree import _tree
import numpy as np
import json


FEATURE_NAMES = [
    "hr_mean",          # bpm
    "hr_std",           # bpm variability (RMSSD)
    "hr_slope",         # delta bpm / second
    "steps_20s",        # steps
    "accel_rms",        # movement intensity
    "accel_peak",       # sudden motion
    "ppg_std",          # photoplethysmogram (PPG) - blood volume pulse variability
    "time_of_day",      # normalized where 1 is 2:30 a.m. [0,1]
]

class DetectionModel:
    def __init__(self):
        self.model = RandomForestClassifier(
            n_estimators=30,
            max_depth=4,
            random_state=42
        )

    def export(self, filename="rf_model.json"):
        forest = []

        for estimator in self.model.estimators_:
            tree = estimator.tree_
            forest.append({
                "feature": getattr(tree, "feature").tolist(),
                "threshold": getattr(tree, "threshold").tolist(),
                "left": getattr(tree, "children_left").tolist(),
                "right": getattr(tree, "children_right").tolist(),
                "value": getattr(tree, "value").squeeze(axis=1).tolist()
            })

        with open(filename, "w") as f:
            json.dump(forest, f)

        print(f"Exported {len(forest)} trees → {filename}")

    """
    Runs detection on the given window of sensor data.
    Accepts a feature_dict (dict mapping feature names) and returns a confidence score.
    """
    def predict(self, feature_dict) -> float:
        x = np.array([
            feature_dict[name] for name in FEATURE_NAMES
        ]).reshape(1, -1)

        return self.model.predict_proba(x)[0][1]
    
    """
    Trains the model with the provided data and labels.
    Accepts (n_samples x features) and (n_samples).
    """
    def train(self, X_train, y_train):
        self.model.fit(X_train, y_train)


# Synthetic samples
SAMPLES = [
    (0, [93.525, 2.0816659994661326, 0.4358974358974359, 7, 11.196551120264434, 24.884628702246506, 4.827329645019297, 0.812013888888889]),
    (0, [92.8, 2.0443794118233485, 0.28205128205128205, 5, 11.220720106113566, 24.884628702246506, 4.814569124793603, 0.012025462962963]),
    (0, [100.5, 2.142368686697878, -0.52564102564102564, 10, 26.910407336704832, 92.4629608619655, 4.871614770479965, 0.7028703703703704]),
    (0, [113.55, 2.0693117894728386, 0.4358974358974359, 22, 29.00302830928387, 92.4629608619655, 5.636178226366403, 0.3028009259259259]),
    (0, [120.4, 1.941450686788302, 0.3333333333333333, 32, 28.72133647914066, 91.62337973235826, 8.864017983813246, 0.197673611111111]),
    (1, [123.525, 2.081218284, 0.7358974358974359, 7, 11.196551120264434, 24.884628702246506, 8.827329645019297, 0.912013888888889]),
    (1, [110, 2.123421, 0.3358974358974359, 2, 5.196551120264434, 14.884628702246506, 7.90124, 0.812013888888889]),
    (1, [133.582, 1, 0.01, 10, 10.67, 40, 6.3, 0.99]),
    (1, [135.124, 3.1, 0.5, 14, 9.123, 51, 4.3, 0.75]),
]

BASE_STD = np.array([1.5, 0.1, 0.05, 1.0, 0.5, 1.5, 0.3, 0.02])

def generate_rows(mean_vec, n=100):
    mean_vec = np.array(mean_vec, dtype=float)
    std_vec = np.maximum(np.abs(mean_vec) * 0.02, BASE_STD)
    rows = np.random.normal(loc=mean_vec, scale=std_vec, size=(n, mean_vec.size))
    rows[:, 0] = np.clip(rows[:, 0], 50, 180)   # hr_mean
    rows[:, 1] = np.clip(rows[:, 1], 0, None)   # hr_std
    rows[:, 3] = np.clip(rows[:, 3], 0, None)   # steps_20s
    rows[:, 4] = np.clip(rows[:, 4], 0, None)   # accel_rms
    rows[:, 5] = np.clip(rows[:, 5], 0, None)   # accel_peak
    rows[:, 6] = np.clip(rows[:, 6], 0, None)   # ppg_std
    rows[:, 7] = np.clip(rows[:, 7], 0, 1)      # time_of_day
    return rows

X_train = np.vstack([generate_rows(sample, n=100) for _, sample in SAMPLES])
y_train = np.array([label for label, _ in SAMPLES for _ in range(100)])

# Example with only two features

# FEATURE_NAMES = [
#     "hr_mean",
#     "steps_20s"
# ]

# # Low hr is 60 +- 10 bpm
# # High hr is 100 +- 10 bpm

# # Low steps is 3 +- 2 steps per 20s
# # High steps is 15 +- 5 steps per 20s

# low_hr_low_steps = [
#     np.random.normal(70, 5, 100),
#     np.random.normal(5, 2, 100),
# ]
# low_hr_low_steps_matrix = np.column_stack(low_hr_low_steps)

# high_hr_high_steps = [
#     np.random.normal(100, 10, 100),
#     np.random.normal(20, 5, 100),
# ]
# high_hr_high_steps_matrix = np.column_stack(high_hr_high_steps)

# high_hr_low_steps = [
#     np.random.normal(90, 8, 100),
#     np.random.normal(2, 1, 100),
# ]
# high_hr_low_steps_matrix = np.column_stack(high_hr_low_steps)

# low_hr_high_steps = [
#     np.random.normal(65, 5, 100),
#     np.random.normal(15, 4, 100),
# ]
# low_hr_high_steps_matrix = np.column_stack(low_hr_high_steps)

# X_train = np.vstack([
#     high_hr_low_steps_matrix,
#     low_hr_high_steps_matrix,
#     low_hr_low_steps_matrix,
#     high_hr_high_steps_matrix,
# ])
# y_train = [1] * 100 + [0] * 300

model = DetectionModel()
model.train(X_train, y_train)

# test_feature = { "hr_mean": 90, "steps_20s": 5 }

# confidence = model.predict(test_feature)
# print(f"Anomaly confidence: {confidence:.4f}")

model.export()
