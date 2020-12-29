(function() { //IIFE
	$(document).ready(function(){
		
		updateQuestionsFieldset(); // to create at least the first question
		
		$("#numQuest").bind("input", updateQuestionsFieldset);		
		
		function updateQuestionsFieldset(){
			console.log("update on questions' number invoked");
			var numQuest = $("#numerQuest").val();
			console.log("retreived " + numQuest + " as a number of questions");
			
			var i = 0;
			
			for(i = 0; i < numQuest; i++){
				var newInput = $("<input type='text' name='question" + i + "' placeholder='Insert " + i + " question' required>");
				$(newInput).appendTo("#marketingQuestions");
			}
		}
		
	});
})();