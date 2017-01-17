from django.shortcuts import render
from django.http import HttpResponse
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from itertools import chain

import json

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
def saveTrip(request, userName, name, bikeType, phonePlacement, isPublic):
	if request.method == 'POST':

		user = User.objects.filter(user_name=userName)

		if (user and request.body):
			boolIsPublic = False
			if isPublic == 'true':
				boolIsPublic = True

			Trip.objects.create(
				user_fkey = user.get(), 
				name = name, 
				bike_used = bikeType, 
				phone_placement = phonePlacement, 
				is_public = boolIsPublic, 
				trip_data = request.body)

			trip_object = json.loads(str(request.body, "utf-8"))
			trip_array = trip_object["trip_data"]

			for record in trip_array:
				saveRating(round(record["latitude"], 4), round(record["longitude"], 4), record["rating"])

			return HttpResponse('true')

		return HttpResponse('false_invalid_form')

	return HttpResponse('false_post')

def saveRating(latitude, longitude, rating):
	try:
		ratingObject = Rating.objects.get(latitude=latitude, longitude=longitude)
		ratingObject.rating += rating
		ratingObject.count += 1
		ratingObject.save()
	except Rating.DoesNotExist:
		Rating.objects.create(latitude=latitude, longitude=longitude, rating=rating, count = 1)

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
		tripResponse = {
			"name": tripObject.name,
			"bike_used": tripObject.bike_used,
			"phone_placement": tripObject.phone_placement,
			"trip_data": tripObject.trip_data
		}

	return JsonResponse(tripResponse)
