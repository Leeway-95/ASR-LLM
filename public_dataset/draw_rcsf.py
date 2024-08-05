import pybaobabdt
import pandas as pd
from scipy.io import arff
from sklearn.tree import DecisionTreeClassifier

data = arff.loadarff('test_rcsf.arff')
df = pd.DataFrame(data[0])

y = list(df['Class'])
features = list(df.columns)
features.remove('Class')
X = df.loc[:, features]

clf = DecisionTreeClassifier().fit(X, y)

ax = pybaobabdt.drawTree(clf, size=10, dpi=1440, features=features)
ax.get_figure().savefig('test_rcsf.png', format='png', dpi=1440, transparent=True)
