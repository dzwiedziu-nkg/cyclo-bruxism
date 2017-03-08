from django.shortcuts import render
from django.http import HttpResponse
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from itertools import chain
from django.core.files.base import ContentFile
from django.core.files import File

import json
import math
import logging

from .models import User
from .models import Trip
from .models import Rating
from .forms import DocumentForm

def index(request):
	return HttpResponse("Hello world")

# Logowanie. Zwraca 'true' jeżeli użytkownik i chasło zgadzają się, 'false' jeżeli nie
def login(request, userName, password):
	try:
		User.objects.get(user_name=userName, password=password)
	except User.DoesNotExist:
		return HttpResponse('false')
	return HttpResponse('true')

# Rejestracja konta. Zwraca 'true' jeżeli udało się założyć konto, 'false' jeżeli nie ( nazwa już istnieje )
def register(request, userName, password):
	userCheck = User.objects.filter(user_name=userName)
	if userCheck:
		return HttpResponse('false')
	else:
		User.objects.create(user_name=userName, password=password)
		return HttpResponse('true')

# Dodanie informacji o podróży. Zwraca 'true' jeżeli operacja przebiegła pomyślnie, 
# 'false_invalid_form' jeżeli załącznik nie jest poprawny
# 'false_post' jeżeli nie połączono się z użyciem metody "POST"
@csrf_exempt 
def saveTrip(request, userName, name, bikeType, phonePlacement, isPublic, tripDate):
	if request.method == 'POST':

		user = User.objects.filter(user_name=userName)

		if (user and request.body):
			filteredTrip = Trip.objects.filter(name=name, date=tripDate)

			# Dodanie danych do już istniejącej podróży
			if (filteredTrip):
				tripFromDatabase = filteredTrip.get()
				file = tripFromDatabase.trip_data

				trip_object_old = json.loads(str(file.read(), "utf-8").replace("'",'"'))
				trip_array_old = trip_object_old["trip_data"]

				trip_object_new = json.loads(str(request.body, "utf-8"))
				trip_array_new = trip_object_new["trip_data"]

				tripObjectToSave = {}

				# Przetworzenie wyników i dodanie ich do odpowiednich obiektów
				for record in trip_array_new:
					latitude = record["latitude"]
					longitude = record["longitude"]
					rating = ratingCalculation(record["soundNoise"], record["shake"])

					record = {
						"latitude": latitude,
						"longitude": longitude,
						"rating": rating
					}

					saveRating(latitude, longitude, rating)
					trip_array_old.append(record)

				tripObjectToSave["trip_data"] = trip_array_old

				# Utworzenie nowego pliku z informacjami o podróży, oraz usunięcie starego
				appendedFile = ContentFile(str(tripObjectToSave))
				appendedFile.name = name

				tripFromDatabase.trip_data.delete()
				tripFromDatabase.trip_data = appendedFile
				tripFromDatabase.save()

			# Dodanie do bazy nowej podróży
			else:
				boolIsPublic = False
				if isPublic == 'true':
					boolIsPublic = True

				trip_object = json.loads(str(request.body, "utf-8"))
				trip_array = trip_object["trip_data"]

				tripObjectToSave = {}
				tripArrayToSave = []

				# Przetworzenie wyników i dodanie ich do odpowiednich obiektów
				for record in trip_array:
					latitude = record["latitude"]
					longitude = record["longitude"]
					rating = ratingCalculation(record["soundNoise"], record["shake"])

					saveRating(latitude, longitude, rating)

					record = {
						"latitude": latitude,
						"longitude": longitude,
						"rating": rating
					}

					tripArrayToSave.append(record)

				tripObjectToSave["trip_data"] = tripArrayToSave

				# Utworzenie nowego obiektu "Trip"
				newTrip = Trip(
					user_fkey = user.get(), 
					name = name,
					date = tripDate,
					bike_used = bikeType, 
					phone_placement = phonePlacement, 
					is_public = boolIsPublic)

				# Utworzenie pliku w którym przechowywane są inormacje na temat podróży
				uploaded_file = ContentFile(str(tripObjectToSave))
				uploaded_file.name = name

				# Zapisanie nowego wpisu do bazy danych
				newTrip.trip_data = uploaded_file
				newTrip.save()

			return HttpResponse('true')

		return HttpResponse('false_invalid_form')

	return HttpResponse('false_post')

# Zwraca ocenę z zakresu 1-10. 1 to najlepsza, 10 najgorsza
def ratingCalculation(soundNoise, shake):
	count = 0
	noiseGrade = 0.0
	shakeGrade = 0.0

	# wystawienie oceny na podstawie dźwięku
	if (not math.isnan(soundNoise)):
		count += 1
		noiseGrade = (soundNoise - 10.0) / 10

		# normalizacja danych dźwiękowych do oceny z zakresu 1-10
		noiseGrade = min(noiseGrade, 10.0)
		noiseGrade = max(noiseGrade, 1.0)

	# wystawienie oceny na podstawie wstrząsów
	if (not math.isnan(shake)):
		count += 1
		shakeGrade = shake * 4 / 10

		# normalizacja danych do oceny z zakresu 1-10
		shakeGrade = min(shakeGrade, 10.0);
		shakeGrade = max(shakeGrade, 1.0);

	grade = 0.0
	if ( count > 0 ):
		grade = (noiseGrade + shakeGrade) / count

	grade = round(grade, 1)
	return grade

def saveRating(latitude, longitude, rating):
	try:
		ratingObject = Rating.objects.get(latitude=round(latitude, 4), longitude=round(longitude, 4))
		ratingObject.rating += rating
		ratingObject.count += 1
		ratingObject.save()
	except Rating.DoesNotExist:
		Rating.objects.create(latitude=round(latitude, 4), longitude=round(longitude, 4), rating=rating, count = 1)

	return

# Zwraca listę podróży z bazy danych.
# Dla mode = 'userOnly' zwraca podróże tylko dla podanego użytkownika
# Dla mode = 'allUsers' zwraca podróże podanego użytkownika, oraz upublicznione podróże innych
def listTrip(request, userName, mode):
	userCheck = User.objects.filter(user_name=userName)
	if (not userCheck):
		return JsonResponse({})

	trip_response = {}
	trip_records = []
		
	if (mode == 'userOnly'):
		tripList = Trip.objects.filter(
			user_fkey = userCheck
		)

	elif (mode == 'allUsers'):
		tripListUser = Trip.objects.filter(
			user_fkey = userCheck
		)

		tripListOthers = Trip.objects.exclude(
			user_fkey = userCheck
		).filter(
			is_public = True
		)
		
		tripList = list(chain(tripListUser, tripListOthers))

	# Niepoprawny mode, zwracamy pusty obiekt JSON
	else: 
		return JsonResponse(trip_response)

	for trip in tripList:
		record = {
			"id": trip.id,
			"name": trip.name
		}

		trip_records.append(record)

	trip_response["array"] = trip_records

	return JsonResponse(trip_response)

# Zwraca podróż o podanym id
def getTrip(request, id):
	tripObject = Trip.objects.get(id=id)

	tripResponse = {}

	if (tripObject):
		file = tripObject.trip_data
		trip_data_lines = str(file.read(), "utf-8")

		tripResponse = {
			"name": tripObject.name,
			"bike_used": tripObject.bike_used,
			"phone_placement": tripObject.phone_placement,
			"trip_data": trip_data_lines
		}

	return JsonResponse(tripResponse)

# Zwraca rating
def getRating(request):
	ratingObjects = Rating.objects.filter()

	ratingResponse = {}
	ratingRecords = []

	for rating in ratingObjects:
		record = {
			"latitude": rating.latitude,
			"longitude": rating.longitude,
			"rating": round((rating.rating / rating.count), 2)
		}

		ratingRecords.append(record)

	ratingResponse["array"] = ratingRecords
	return JsonResponse(ratingResponse)
