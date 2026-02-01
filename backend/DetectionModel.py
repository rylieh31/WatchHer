"""
Model Inputs:
- HR deviation from baseline
- HR slope (Delta HR / second)
- HRV (RMSSD)
- HR / step ratio
- Delta step
- Step interval std
- Respiration rate
- Respiration irregularity
- Respiration vs HR mismatch
- Time of day
"""

from sklearn.ensemble import RandomForestClassifier
import numpy as np


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

    """
    Runs detection on the given window of sensor data.
    Accepts a feature_dict (dict mapping feature names) and returns a confidence score.
    """
    def predict(self, feature_dict) -> float:
        return self.model.predict_proba([feature_dict])[0][1]
    
    """
    Trains the model with the provided data and labels.
    Accepts (n_samples x features) and (n_samples).
    """
    def train(self, X_train, y_train):
        self.model.fit(X_train, y_train)



# Example with only two features

TEST_FEATURES = {
    "hr_mean",
    "steps_20s"
}

# Low hr is 60 +- 10 bpm
# High hr is 100 +- 10 bpm

# Low steps is 3 +- 2 steps per 20s
# High steps is 15 +- 5 steps per 20s

low_hr_low_steps = [
    np.random.normal(70, 5, 100),
    np.random.normal(5, 2, 100),
]
low_hr_low_steps_matrix = np.column_stack(low_hr_low_steps)

high_hr_high_steps = [
    np.random.normal(100, 10, 100),
    np.random.normal(20, 5, 100),
]
high_hr_high_steps_matrix = np.column_stack(high_hr_high_steps)

high_hr_low_steps = [
    np.random.normal(90, 8, 100),
    np.random.normal(2, 1, 100),
]
high_hr_low_steps_matrix = np.column_stack(high_hr_low_steps)

low_hr_high_steps = [
    np.random.normal(65, 5, 100),
    np.random.normal(15, 4, 100),
]
low_hr_high_steps_matrix = np.column_stack(low_hr_high_steps)

X_train = np.vstack([
    high_hr_low_steps_matrix,
    low_hr_high_steps_matrix,
    low_hr_low_steps_matrix,
    high_hr_high_steps_matrix,
])
y_train = [1] * 100 + [0] * 300

model = DetectionModel()
model.train(X_train, y_train)

test_feature = [ 70, 5 ]

confidence = model.predict(test_feature)
print(f"Anomaly confidence: {confidence:.4f}")
