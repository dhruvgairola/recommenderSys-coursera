import operator

def getMovieMap():
    f = open('sample_data.csv')

    allLines = []

    for line in f:
        allLines.append(line)

    cols = allLines[0].split(',')[1:]
    rows = []
    movieMap = {}

    for line in allLines[1:]:
        rowVals = line.split(',')
        rows.append(rowVals[0])
        vals = rowVals[1:]
        for k, v in enumerate(vals):
            if cols[k] not in movieMap:
                movieMap[cols[k]] = {}
            movieMap[cols[k]][rowVals[0]] = v

    return movieMap

def computeMean(movieMap):
    meanMap = {}

    for movie, ratings in movieMap.items():
        totalRating = 0.0
        numberOfRatings = 0

        for user, rating in ratings.items():
            if rating != '' and rating != '\n':
                totalRating += float(rating)
                numberOfRatings += 1

        avgRating = totalRating / numberOfRatings
        meanMap[movie] = avgRating

    sorted_ratings = sorted(meanMap.items(), key=operator.itemgetter(1), reverse=True)
    return sorted_ratings[:5]

def computeRatingsAbove(movieMap, x):
    percMap = {}

    for movie, ratings in movieMap.items():
        all = 0
        above_x = 0

        for user, rating in ratings.items():
            if rating != '' and rating != '\n':
                r = float(rating)
                if r >= x:
                    above_x += 1
                all += 1

        perc = above_x / all
        percMap[movie] = perc

    sorted_ratings = sorted(percMap.items(), key=operator.itemgetter(1), reverse=True)
    return sorted_ratings[:5]

def computeNoOfRatings(movieMap):
    numMap = {}

    for movie, ratings in movieMap.items():
        num = 0

        for user, rating in ratings.items():
            if rating != '' and rating != '\n':
                num += 1

        numMap[movie] = num

    sorted_ratings = sorted(numMap.items(), key=operator.itemgetter(1), reverse=True)
    return sorted_ratings[:5]

def computeCorrelatedMovies(movieMap, selMovie):
    movieRaters = set()

    numMap = {}

    for movie, ratings in movieMap.items():
        if selMovie in movie:
            for user, rating in ratings.items():
                if rating != '' and rating != '\n':
                    movieRaters.add(user)

    for movie, ratings in movieMap.items():
        percSelMovie = 0

        for user, rating in ratings.items():
            if rating != '' and rating != '\n' and user in movieRaters:
                percSelMovie += 1

        numMap[movie] = percSelMovie / len(movieRaters)

    sorted_ratings = sorted(numMap.items(), key=operator.itemgetter(1), reverse=True)
    return sorted_ratings[1:6]

if __name__ == "__main__":
    movieMap = getMovieMap()
    computeMean(movieMap)
    computeRatingsAbove(movieMap, 4)
    computeNoOfRatings(movieMap)
    computeCorrelatedMovies(movieMap, '260')