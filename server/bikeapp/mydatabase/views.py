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
import os

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

				trip_object_new = json.loads(str(request.body, "utf-8"))
				trip_array_new = trip_object_new["trip_data"]

				# Otwarcie pliku do przetworzenia
				with open(file.path, 'rb+') as filehandle:
					filehandle.seek(-2, os.SEEK_END)
					filehandle.truncate()

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

						# Dopisanie wyniku na koniec pliku
						filehandle.write(bytes(', ', 'utf-8'))
						filehandle.write(bytes(str(record), 'utf-8'))

					# Zakończenie pliku
					filehandle.write(bytes(']}', 'utf-8'))

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
		noiseGrade = (soundNoise - 20.0) / 8

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
# Przy dużej ilości wpisów dane są skracane i wysyłane są tylko niektóre wpisy z uśrednioną oceną
def getTrip(request, id):
	tripObject = Trip.objects.get(id=id)

	tripResponse = {}

	if (tripObject):
		file = tripObject.trip_data
		trip_data_lines = str(file.read(), "utf-8")

		trip_data_parsed = json.loads(trip_data_lines.replace("\'", '"'))
		trip_data_array = trip_data_parsed["trip_data"]

		entryCount = len(trip_data_array)
		drawingInterval = 1
		counter = 0
		rating_sum = 0.0

		trip_data_short = {}
		trip_array_short_records = []

		# Dopasowanie rozdzielczości do ilości wpisów
		if (entryCount < 1000):
			drawingInterval = 1
		elif (entryCount < 2000):
			drawingInterval = 2
		elif (entryCount < 5000):
			drawingInterval = 5
		elif (entryCount < 20000):
			drawingInterval = 10
		else:
			drawingInterval = 20

		# Przygotowanie odpowiednich danych do wysłania
		for record in trip_data_array:
			counter += 1
			rating_sum += record["rating"]

			if (counter % drawingInterval == 0):
				rating_final = round(rating_sum / drawingInterval, 1)

				# Zabezpieczenie przed niepoprawnymi wartościami ratingu:
				if (rating_final < 1.0):
					rating_final = 1.0
				if (rating_final > 10.0):
					rating_final = 10.0

				record_final = {
					"latitude": record["latitude"],
					"longitude": record["longitude"],
					"rating": rating_final
				}

				trip_array_short_records.append(record_final)

				counter = 0
				rating_sum = 0.0

		# Zwrócenie skróconej tablicy z recordami
		trip_data_short["trip_data"] = trip_array_short_records
		tripResponse = {
			"name": tripObject.name,
			"bike_used": tripObject.bike_used,
			"phone_placement": tripObject.phone_placement,
			"trip_data": trip_data_short
		}

	return JsonResponse(tripResponse)

# Zwraca rating dla odpowiedniego obszaru geograficznego
def getRating(request, north, south, east, west, resolution):
	ratingObjects = Rating.objects.filter(latitude__lte=north, latitude__gte=south, longitude__gte=east, longitude__lte=west)

	ratingResponse = {}
	ratingRecords = []

	logging.warning(resolution)

	if (resolution == '1'):
		for rating in ratingObjects:
			record = {
				"latitude": rating.latitude,
				"longitude": rating.longitude,
				"rating": round((rating.rating / rating.count), 2)
			}

			ratingRecords.append(record)

	if (resolution == '3'):
		low_res_rating = {}

		for rating in ratingObjects:
			key = ''
			value = [None] * 2

			key = str(round(rating.latitude, 3)) + '-' + str(round(rating.longitude, 3))

			if key in low_res_rating:
				value[0] = low_res_rating[key][0] + 1.0
				value[1] = low_res_rating[key][1] + round((rating.rating / rating.count), 2)
				low_res_rating[key] = value
			else:
				value[0] = 1.0
				value[1] = round((rating.rating / rating.count), 2)
				low_res_rating[key] = value

		for key, value in low_res_rating.items():
			latitude = key.split('-')[0]
			longitude = key.split('-')[1]

			record = {
				"latitude": latitude,
				"longitude": longitude,
				"rating": round((value[1] / value[0]), 2)
			}

			ratingRecords.append(record)

	if (resolution == '5'):
		low_res_rating = {}

		for rating in ratingObjects:
			key = ''
			value = [None] * 2

			key = str(round(rating.latitude, 2)) + '-' + str(round(rating.longitude, 2))

			if key in low_res_rating:
				value[0] = low_res_rating[key][0] + 1.0
				value[1] = low_res_rating[key][1] + round((rating.rating / rating.count), 2)
				low_res_rating[key] = value
			else:
				value[0] = 1.0
				value[1] = round((rating.rating / rating.count), 2)
				low_res_rating[key] = value

		for key, value in low_res_rating.items():
			latitude = key.split('-')[0]
			longitude = key.split('-')[1]

			record = {
				"latitude": latitude,
				"longitude": longitude,
				"rating": round((value[1] / value[0]), 2)
			}

			ratingRecords.append(record)

	ratingResponse["array"] = ratingRecords
	return JsonResponse(ratingResponse)
